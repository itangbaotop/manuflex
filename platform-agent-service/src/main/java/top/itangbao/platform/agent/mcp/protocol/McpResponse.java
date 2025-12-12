package top.itangbao.platform.agent.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class McpResponse extends McpMessage {
    private Object result;
    private McpError error;
    
    @Override
    public String getMethod() {
        return null;
    }
    
    @Data
    @SuperBuilder
    public static class McpError {
        private int code;
        private String message;
        private Object data;
    }
}