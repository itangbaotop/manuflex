package top.itangbao.platform.agent.core;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Agent 任务
 */
@Data
@Builder
public class AgentTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 用户输入
     */
    private String userInput;
    
    /**
     * 租户ID
     */
    private String tenantId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 任务参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 上下文信息
     */
    private Map<String, Object> context;
}