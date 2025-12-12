package top.itangbao.platform.agent.mcp.tool;

import java.util.Map;

public interface McpTool {
    String getName();
    String getDescription();
    Map<String, Object> getInputSchema();
    Object execute(Map<String, Object> arguments);
}