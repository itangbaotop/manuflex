package top.itangbao.platform.agent.dto;

import lombok.Data;
import java.util.Map;

/**
 * Agent 执行结果
 */
@Data
public class AgentResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 结果数据
     */
    private Object data;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 执行时间(毫秒)
     */
    private long executionTime;
    
    /**
     * 额外信息
     */
    private Map<String, Object> metadata;
    
    public static AgentResult success(String message, Object data) {
        AgentResult result = new AgentResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    
    public static AgentResult failure(String error) {
        AgentResult result = new AgentResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
}