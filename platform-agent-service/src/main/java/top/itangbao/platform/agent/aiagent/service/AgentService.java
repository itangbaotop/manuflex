package top.itangbao.platform.agent.aiagent.service;

import top.itangbao.platform.agent.dto.AgentResult;

import java.util.List;

/**
 * Agent 管理服务
 */
public interface AgentService {

    AgentResult executeTask(String userInput, String tenantId, String userId);

}