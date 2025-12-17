package top.itangbao.platform.agent.aiagent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RouterAssistant {

    @SystemMessage("""
        你是一个意图分类器。你的唯一任务是分析用户的输入，并将其归类为以下类别之一：
        
        1. FORM: 用户想要创建、设计、修改表单或数据模型（如"创建请假表单"、"加个字段"）。
        2. WORKFLOW: 用户想要查询流程定义、发起流程、处理任务（如"我要请假"、"有哪些流程"）。
        3. DATA: 用户想要查询业务数据、统计数量、分析报表（如"有多少个订单"、"查找张三的申请"）。
        4. KNOWLEDGE: 用户询问具体的业务规则、SOP、操作指南、历史文档（如"报销标准是什么"、"如何处理退货"）。
        5. CHAT: 用户进行打招呼、闲聊或提出与业务系统无关的通用问题。
        
        请直接返回类别名称（枚举值），不要输出任何其他解释性文字。
        """)
    AgentIntent classify(@UserMessage String userMessage);
}