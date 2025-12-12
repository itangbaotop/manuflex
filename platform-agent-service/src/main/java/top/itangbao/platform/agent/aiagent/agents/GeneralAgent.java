package top.itangbao.platform.agent.aiagent.agents;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.core.AgentTask;

/**
 * 通用智能助手 Agent
 * 处理一般性问题和平台使用指导
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneralAgent implements Agent {
    
    private final ChatModel chatModel;
    
    @Override
    public String getType() {
        return "GENERAL";
    }
    
    @Override
    public String getName() {
        return "通用智能助手";
    }
    
    @Override
    public String getDescription() {
        return "回答一般性问题，提供平台使用指导";
    }
    
    @Override
    public boolean canHandle(AgentTask task) {
        // 作为兜底Agent，处理其他Agent无法处理的任务
        return true;
    }
    
    @Override
    public AgentResult execute(AgentTask task) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("GeneralAgent executing task: {}", task.getTaskId());
            
            String prompt = buildGeneralPrompt(task.getUserInput());
            String response = chatModel.chat(prompt);
            
            long executionTime = System.currentTimeMillis() - startTime;
            AgentResult result = AgentResult.success("回答完成", response);
            result.setExecutionTime(executionTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("GeneralAgent execution failed", e);
            return AgentResult.failure("处理失败: " + e.getMessage());
        }
    }
    
    private String buildGeneralPrompt(String userInput) {
        return String.format("""
            你是ManuFlex制造业PaaS平台的智能助手。请根据用户问题提供专业的回答。
            
            平台功能包括：
            - 元数据管理：动态表单和数据模型
            - 工作流引擎：业务流程自动化
            - 数据管理：动态数据存储和查询
            - 文件管理：文档存储和知识库
            - 身份认证：用户和权限管理
            - AI助手：智能化业务处理
            
            用户问题：%s
            
            请提供准确、专业的回答，如果涉及具体操作，请给出详细步骤。
            """, userInput);
    }
}