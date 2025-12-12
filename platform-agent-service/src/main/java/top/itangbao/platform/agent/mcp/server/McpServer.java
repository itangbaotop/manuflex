package top.itangbao.platform.agent.mcp.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.itangbao.platform.agent.mcp.protocol.McpRequest;
import top.itangbao.platform.agent.mcp.protocol.McpResponse;
import top.itangbao.platform.agent.mcp.tool.McpTool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServer {
    
    private final List<McpTool> tools;
    
    public McpResponse handleRequest(McpRequest request) {
        try {
            return switch (request.getMethod()) {
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolCall(request);
                default -> McpResponse.builder()
                    .id(request.getId())
                    .error(McpResponse.McpError.builder()
                        .code(-32601)
                        .message("Method not found: " + request.getMethod())
                        .build())
                    .build();
            };
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return McpResponse.builder()
                .id(request.getId())
                .error(McpResponse.McpError.builder()
                    .code(-32603)
                    .message("Internal error: " + e.getMessage())
                    .build())
                .build();
        }
    }
    
    private McpResponse handleToolsList() {
        List<Map<String, Object>> toolList = tools.stream()
            .map(tool -> Map.of(
                "name", tool.getName(),
                "description", tool.getDescription(),
                "inputSchema", tool.getInputSchema()
            ))
            .collect(Collectors.toList());
            
        return McpResponse.builder()
            .result(Map.of("tools", toolList))
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private McpResponse handleToolCall(McpRequest request) {
        Map<String, Object> params = (Map<String, Object>) request.getParams();
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        McpTool tool = tools.stream()
            .filter(t -> t.getName().equals(toolName))
            .findFirst()
            .orElse(null);
            
        if (tool == null) {
            return McpResponse.builder()
                .id(request.getId())
                .error(McpResponse.McpError.builder()
                    .code(-32602)
                    .message("Tool not found: " + toolName)
                    .build())
                .build();
        }
        
        Object result = tool.execute(arguments);
        return McpResponse.builder()
            .id(request.getId())
            .result(result)
            .build();
    }
}