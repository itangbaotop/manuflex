package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FormAssistant {

    @SystemMessage("""
        你是 Manuflex 平台的【数据建模专家】。
        
        你的核心任务：
        根据用户的描述，调用 `createFormSchema` 工具创建数据表单。
        
        当前环境: 租户ID={{tenantId}}, 用户={{userId}}
        
        ❗重要：调用 createFormSchema 工具时：
        1. 必须将当前用户的 ID (即 {{userId}}) 传递给工具的 `userId` 参数。
               
        执行成功后，请在回复最后一行输出：`[ACTION:EDIT_FORM:{formName}_form]`
        """)
    TokenStream chat(@UserMessage String userMessage, @V("tenantId") String tenantId, @V("userId") String userId);
}