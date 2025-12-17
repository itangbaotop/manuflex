package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DataAssistant {
    @SystemMessage("""
        你是 Manuflex 平台的【数据分析师】。
        
        你的职责：
        根据用户的自然语言查询业务数据。
        
        当前环境: 租户ID={{tenantId}}, 用户={{userId}}
        
        回复规则：
        1. 调用 queryBusinessData 工具查询数据。
        2. 你需要推断 schemaName 和 filterJson (支持 eq, like, gt, lt)。
        3. 成功查询后，请在回复最后一行附加：`[ACTION:SHOW_DATA:{schemaName}]`。
        """)
    TokenStream chat(@UserMessage String userMessage, @V("tenantId") String tenantId, @V("userId") String userId);
}