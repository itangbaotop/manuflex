package top.itangbao.platform.agent.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import top.itangbao.platform.agent.aiagent.tools.DataAnalysisTools;
import top.itangbao.platform.agent.aiagent.tools.KnowledgeBaseTools;
import top.itangbao.platform.agent.aiagent.tools.SchemaTools;
import top.itangbao.platform.agent.aiagent.tools.WorkflowTools;
import top.itangbao.platform.agent.mcp.protocol.McpRequest;
import top.itangbao.platform.agent.mcp.protocol.McpResponse;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能 MCP Server (修复版)
 * 使用原生反射替代内部 API，实现自动发现并暴露 @Tool 方法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServer {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    // 缓存工具定义：工具名 -> 规范
    private final Map<String, ToolSpecification> toolSpecs = new HashMap<>();

    // 缓存工具执行信息：工具名 -> (Bean实例, Method对象)
    private final Map<String, ToolMethod> toolMethods = new HashMap<>();

    // 内部记录类，用于保存反射调用的目标
    private record ToolMethod(Object bean, Method method) {}

    @PostConstruct
    public void init() {
        log.info("MCP Server initializing... scanning for @Tool beans");

        // 注册 SchemaTools
        registerToolsFromBean(applicationContext.getBean(SchemaTools.class));

        // 如果有其他 Tool Bean，在这里继续注册，例如：
        try {
            registerToolsFromBean(applicationContext.getBean(WorkflowTools.class));
        } catch (Exception e) {
            log.warn("WorkflowTools bean not found, skipping registration.");
        }

        try {
            registerToolsFromBean(applicationContext.getBean(KnowledgeBaseTools.class));
        } catch (Exception e) {
            log.warn("KnowledgeBaseTools bean not found");
        }

        try {
            registerToolsFromBean(applicationContext.getBean(DataAnalysisTools.class));
        } catch (Exception e) {
            log.warn("DataAnalysisTools bean not found");
        }

        log.info("MCP Server initialized. Loaded {} tools: {}", toolSpecs.size(), toolSpecs.keySet());
    }

    private void registerToolsFromBean(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                try {
                    // 1. 生成工具规范 (JSON Schema)
                    ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                    toolSpecs.put(spec.name(), spec);

                    // 2. 缓存方法引用，供后续反射调用
                    toolMethods.put(spec.name(), new ToolMethod(bean, method));

                } catch (Exception e) {
                    log.error("Failed to register tool from method: {}", method.getName(), e);
                }
            }
        }
    }

    public McpResponse handleRequest(McpRequest request) {
        try {
            return switch (request.getMethod()) {
                case "tools/list" -> handleToolsList(request.getId());
                case "tools/call" -> handleToolCall(request);
                default -> error(request.getId(), -32601, "Method not found: " + request.getMethod());
            };
        } catch (Exception e) {
            log.error("MCP Request Error", e);
            return error(request.getId(), -32603, "Internal error: " + e.getMessage());
        }
    }

    private McpResponse handleToolsList(String id) {
        List<Map<String, Object>> toolsList = new ArrayList<>();

        for (ToolSpecification spec : toolSpecs.values()) {
            toolsList.add(Map.of(
                    "name", spec.name(),
                    "description", spec.description() != null ? spec.description() : "",
                    "inputSchema", spec.parameters()
            ));
        }

        return McpResponse.builder()
                .id(id)
                .result(Map.of("tools", toolsList))
                .build();
    }

    private McpResponse handleToolCall(McpRequest request) {
        Map<String, Object> params = (Map<String, Object>) request.getParams();
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        ToolMethod toolMethod = toolMethods.get(toolName);
        if (toolMethod == null) {
            return error(request.getId(), -32602, "Tool not found: " + toolName);
        }

        try {
            log.info("MCP Executing tool: {} with args: {}", toolName, arguments);

            // 使用反射执行方法
            Object result = executeReflectively(toolMethod, arguments);

            // 将结果转为 String 返回
            String resultStr = result != null ? String.valueOf(result) : "execution completed";

            return McpResponse.builder()
                    .id(request.getId())
                    .result(Map.of("content", List.of(Map.of("type", "text", "text", resultStr))))
                    .build();

        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return error(request.getId(), -32000, "Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 核心反射逻辑：将 Map 参数映射到 Method 参数
     */
    private Object executeReflectively(ToolMethod toolMethod, Map<String, Object> args) throws Exception {
        Method method = toolMethod.method();
        Parameter[] parameters = method.getParameters();
        Object[] invokeArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];

            // 1. 获取参数名 (优先使用 @P 注解，其次尝试获取参数名)
            String paramName = p.getName();
            P annotation = p.getAnnotation(P.class);
            if (annotation != null && annotation.value() != null && !annotation.value().isEmpty()) {
                // 注意：LangChain4j 的 @P 可能不包含 value 属性，或者 value 是描述。
                // 通常 ToolSpecifications 生成 Schema 时会使用反射的参数名。
                // 只要 pom.xml 配置了 -parameters，p.getName() 就是准确的。
            }

            // 2. 从参数 Map 中取值
            Object val = args.get(paramName);

            // 3. 类型转换
            if (val != null) {
                // 特殊处理：如果方法需要 String 但传入了 Map/List (JSON对象)，则转为 JSON 字符串
                if (p.getType().equals(String.class) && (val instanceof Map || val instanceof List)) {
                    invokeArgs[i] = objectMapper.writeValueAsString(val);
                } else {
                    // 使用 Jackson 进行常规类型转换 (如 Integer -> int, String -> Enum)
                    invokeArgs[i] = objectMapper.convertValue(val, p.getType());
                }
            } else {
                invokeArgs[i] = null;
            }
        }

        return method.invoke(toolMethod.bean(), invokeArgs);
    }

    private McpResponse error(String id, int code, String message) {
        return McpResponse.builder()
                .id(id)
                .error(McpResponse.McpError.builder().code(code).message(message).build())
                .build();
    }
}