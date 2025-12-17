package top.itangbao.platform.agent.aiagent.service;

public enum AgentIntent {
    FORM("表单建模与设计"),
    WORKFLOW("流程查询与管理"),
    DATA("数据查询与分析"),
    KNOWLEDGE("知识库问答与SOP"),
    CHAT("闲聊与通用问题");

    private final String description;

    AgentIntent(String description) {
        this.description = description;
    }
}