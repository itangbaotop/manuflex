package top.itangbao.platform.agent.aiagent.agents;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.core.AgentTask;

/**
 * 工作流生成 Agent
 * 根据用户描述自动生成业务流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAgent implements Agent {
    
    private final ChatModel chatModel;
    
    @Override
    public String getType() {
        return "WORKFLOW";
    }
    
    @Override
    public String getName() {
        return "工作流生成助手";
    }
    
    @Override
    public String getDescription() {
        return "根据用户描述自动生成业务流程和工作流";
    }
    
    @Override
    public boolean canHandle(AgentTask task) {
        return "WORKFLOW".equals(task.getTaskType()) || 
               task.getUserInput().contains("流程") || 
               task.getUserInput().contains("工作流") ||
               task.getUserInput().contains("workflow");
    }
    
    @Override
    public AgentResult execute(AgentTask task) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("WorkflowAgent executing task: {}", task.getTaskId());
            
            String prompt = buildWorkflowGenerationPrompt(task.getUserInput());
            String response = chatModel.chat(prompt);
            
            // TODO: 解析AI响应，调用workflow-service创建实际的流程
            
            long executionTime = System.currentTimeMillis() - startTime;
            AgentResult result = AgentResult.success("工作流生成完成", response);
            result.setExecutionTime(executionTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("WorkflowAgent execution failed", e);
            return AgentResult.failure("工作流生成失败: " + e.getMessage());
        }
    }
    
    private String buildWorkflowGenerationPrompt(String userInput) {
        return String.format("""
            你是一个专业的业务流程设计助手。请根据用户需求生成工作流设计方案。
            
            用户需求：%s
            
            请按以下JSON格式返回工作流设计：
            {
              "processName": "流程名称",
              "description": "流程描述",
              "steps": [
                {
                  "id": "步骤ID",
                  "name": "步骤名称", 
                  "type": "步骤类型(USER_TASK/SERVICE_TASK/GATEWAY等)",
                  "assignee": "执行人(仅USER_TASK需要)",
                  "condition": "条件表达式(仅GATEWAY需要)"
                }
              ],
              "connections": [
                {
                  "from": "起始步骤ID",
                  "to": "目标步骤ID"
                }
              ]
            }
            
            请确保返回有效的JSON格式。
            """, userInput);
    }
}