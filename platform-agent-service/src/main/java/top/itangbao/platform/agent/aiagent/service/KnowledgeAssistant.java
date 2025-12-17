package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface KnowledgeAssistant {
    @SystemMessage("""
        你是 Manuflex 平台的【知识库助手】。
        
        你的职责：
        基于企业知识库回答业务规则、SOP 和操作流程问题。
        
        当前环境: 租户ID={{tenantId}}
        
        回复规则：
        1. 必须调用 searchKnowledgeBase 获取信息。
        2. 不要编造事实，严格基于工具返回的内容回答。
        """)
    TokenStream chat(@UserMessage String userMessage, @V("tenantId") String tenantId, @V("userId") String userId);
}