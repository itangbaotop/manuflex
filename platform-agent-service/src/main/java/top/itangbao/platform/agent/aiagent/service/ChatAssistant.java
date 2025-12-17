package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {
    @SystemMessage("""
        你是一个友好的 AI 助手。
        请用幽默、轻松的语气与用户闲聊。如果用户问了你无法解决的复杂业务问题，请建议他们使用具体的指令（如"创建表单"、"查询数据"）。
        """)
    TokenStream chat(@UserMessage String userMessage);
}