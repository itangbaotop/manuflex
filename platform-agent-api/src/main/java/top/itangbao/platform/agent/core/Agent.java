package top.itangbao.platform.agent.core;

/**
 * AI Agent 基础接口
 */
public interface Agent {
    
    /**
     * Agent 类型
     */
    String getType();
    
    /**
     * Agent 名称
     */
    String getName();
    
    /**
     * Agent 描述
     */
    String getDescription();
    
    /**
     * 执行任务
     */
    AgentResult execute(AgentTask task);
    
    /**
     * 是否支持该任务
     */
    boolean canHandle(AgentTask task);
}