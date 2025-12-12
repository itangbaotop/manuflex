package top.itangbao.platform.agent.mcp.protocol;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * MCP 消息基类
 */
@Data
@SuperBuilder
public abstract class McpMessage {
    private String id;
    private String jsonrpc = "2.0";
    
    public abstract String getMethod();
}