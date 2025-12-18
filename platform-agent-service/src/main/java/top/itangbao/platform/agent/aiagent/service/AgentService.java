package top.itangbao.platform.agent.aiagent.service;

import reactor.core.publisher.Flux;
import top.itangbao.platform.agent.dto.AgentResult;

import java.util.List;

/**
 * Agent 管理服务
 */
public interface AgentService {

    Flux<String> executeTaskStream(String userInput, String imageBase64, String tenantId, String userId);

    AgentResult executeTask(String userInput, String tenantId, String userId);

}