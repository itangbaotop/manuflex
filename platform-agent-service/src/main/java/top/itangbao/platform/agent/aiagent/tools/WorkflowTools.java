package top.itangbao.platform.agent.aiagent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.workflow.api.client.WorkflowServiceFeignClient;
import top.itangbao.platform.workflow.api.dto.ProcessDefinitionResponse;
import top.itangbao.platform.workflow.api.dto.ProcessInstanceResponse;
import top.itangbao.platform.workflow.api.dto.StartProcessRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流工具集
 * 赋予 AI 查询和操作流程引擎的能力
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowTools {

    private final WorkflowServiceFeignClient workflowClient;
    private final ObjectMapper objectMapper;

    /**
     * AI 调用此工具来了解当前系统中有哪些可用的业务流程。
     */
    @Tool("查询系统支持的所有业务流程列表，返回流程名称和Key")
    public String listProcessDefinitions(
            @dev.langchain4j.agent.tool.P("租户ID") String tenantId) {
        try {
            // 调用 Workflow Service 获取最新版本的流程定义
            List<ProcessDefinitionResponse> definitions = workflowClient.getProcessDefinitions(null, tenantId, true);

            if (definitions.isEmpty()) {
                return "当前没有可用的业务流程。";
            }

            // 格式化输出，让 AI 容易理解
            return definitions.stream()
                    .map(def -> String.format("- 流程名称: %s, Key: %s, 版本: v%d", def.getName(), def.getKey(), def.getVersion()))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            log.error("查询流程定义失败", e);
            return "查询流程列表失败: " + e.getMessage();
        }
    }

    /**
     * AI 调用此工具来启动一个新的流程实例。
     */
    @Tool("启动一个新的业务流程实例")
    public String startProcess(
            @dev.langchain4j.agent.tool.P("流程定义的Key (ProcessDefinitionKey)，例如 'leave_process'") String processKey,
            @dev.langchain4j.agent.tool.P("业务关联Key (BusinessKey)，可选，通常用于关联具体业务单据ID") String businessKey,
            @dev.langchain4j.agent.tool.P("流程变量的JSON字符串，例如 '{\"days\": 3, \"reason\": \"事假\"}'") String variablesJson,
            @dev.langchain4j.agent.tool.P("租户ID") String tenantId) {

        log.info("🤖 AI正在启动流程: key={}, businessKey={}", processKey, businessKey);

        try {
            Map<String, Object> variables = Collections.emptyMap();
            if (variablesJson != null && !variablesJson.trim().isEmpty() && !variablesJson.equals("{}")) {
                try {
                    variables = objectMapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    return "启动失败：流程变量 JSON 格式错误";
                }
            }

            StartProcessRequest request = StartProcessRequest.builder()
                    .processDefinitionKey(processKey)
                    .businessKey(businessKey)
                    .tenantId(tenantId)
                    .variables(variables)
                    .build();

            ProcessInstanceResponse instance = workflowClient.startProcessInstance(request);
            return "✅ 流程启动成功！实例ID: " + instance.getId();

        } catch (Exception e) {
            log.error("启动流程失败", e);
            return "❌ 流程启动失败: " + e.getMessage();
        }
    }
}