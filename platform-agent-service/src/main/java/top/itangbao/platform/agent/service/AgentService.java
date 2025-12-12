package top.itangbao.platform.agent.service;

import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;

import java.util.List;

/**
 * Agent 管理服务
 */
public interface AgentService {

    AgentResult executeTask(String userInput, String tenantId, String userId);

    List<Agent> getAllAgents();

}