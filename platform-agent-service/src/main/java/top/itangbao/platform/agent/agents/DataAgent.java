package top.itangbao.platform.agent.agents;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.core.AgentTask;

/**
 * 数据分析 Agent
 * 智能数据分析和处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAgent implements Agent {
    
    private final ChatModel chatModel;
    
    @Override
    public String getType() {
        return "DATA";
    }
    
    @Override
    public String getName() {
        return "数据分析助手";
    }
    
    @Override
    public String getDescription() {
        return "智能数据分析、统计和可视化建议";
    }
    
    @Override
    public boolean canHandle(AgentTask task) {
        return "DATA".equals(task.getTaskType()) || 
               task.getUserInput().contains("数据") || 
               task.getUserInput().contains("分析") ||
               task.getUserInput().contains("统计") ||
               task.getUserInput().contains("报表");
    }
    
    @Override
    public AgentResult execute(AgentTask task) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("DataAgent executing task: {}", task.getTaskId());
            
            String prompt = buildDataAnalysisPrompt(task.getUserInput());
            String response = chatModel.chat(prompt);
            
            // TODO: 解析AI响应，调用data-service进行实际的数据分析
            
            long executionTime = System.currentTimeMillis() - startTime;
            AgentResult result = AgentResult.success("数据分析完成", response);
            result.setExecutionTime(executionTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("DataAgent execution failed", e);
            return AgentResult.failure("数据分析失败: " + e.getMessage());
        }
    }
    
    private String buildDataAnalysisPrompt(String userInput) {
        return String.format("""
            你是一个专业的数据分析师。请根据用户需求提供数据分析方案。
            
            用户需求：%s
            
            请按以下JSON格式返回分析方案：
            {
              "analysisType": "分析类型(TREND/COMPARISON/DISTRIBUTION等)",
              "description": "分析描述",
              "metrics": [
                {
                  "name": "指标名称",
                  "field": "数据字段",
                  "aggregation": "聚合方式(SUM/AVG/COUNT等)"
                }
              ],
              "dimensions": ["维度字段1", "维度字段2"],
              "filters": [
                {
                  "field": "过滤字段",
                  "operator": "操作符(=/>/<等)",
                  "value": "过滤值"
                }
              ],
              "visualization": {
                "type": "图表类型(LINE/BAR/PIE等)",
                "title": "图表标题"
              }
            }
            
            请确保返回有效的JSON格式。
            """, userInput);
    }
}