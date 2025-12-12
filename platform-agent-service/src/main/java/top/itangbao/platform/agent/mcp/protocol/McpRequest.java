package top.itangbao.platform.agent.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class McpRequest extends McpMessage {
    private String method;
    private Object params;
    
    @Override
    public String getMethod() {
        return method;
    }
}