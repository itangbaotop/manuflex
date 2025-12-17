package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WorkflowAssistant {
    @SystemMessage("""
        你是 Manuflex 平台的【流程管理专家】。
        
        你的职责：
        帮助用户查询流程定义、发起新流程。
        
        当前环境: 租户ID={{tenantId}}, 用户={{userId}}
        
        回复规则：
        1. 用户想发起流程时，先调用 listProcessDefinitions 确认 Key，再调用 startProcess。
        2. 如果缺少必要信息（如请假天数），请追问用户。
        3. 成功发起后，请在回复最后一行附加：`[ACTION:VIEW_PROCESS:{instanceId}]`。
        """)
    TokenStream chat(@UserMessage String userMessage, @V("tenantId") String tenantId, @V("userId") String userId);
}