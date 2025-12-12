package top.itangbao.platform.agent.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.core.AgentTask;
import top.itangbao.platform.agent.service.AgentService;

import java.util.List;
import java.util.UUID;

/**
 * Agent 管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    
    private final List<Agent> agents;
    
    /**
     * 执行任务
     */
    public AgentResult executeTask(String userInput, String tenantId, String userId) {
        // 创建任务
        AgentTask task = new AgentTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserInput(userInput);
        task.setTenantId(tenantId);
        task.setUserId(userId);

        // 推断任务类型
        task.setTaskType(inferTaskType(userInput));

        // 智能路由 - 找到合适的Agent
        Agent selectedAgent = findSuitableAgent(task);

        if (selectedAgent == null) {
            return AgentResult.failure("未找到合适的Agent处理该任务");
        }

        log.info("Selected agent: {} for task: {}, inferred type: {}",
                selectedAgent.getName(), task.getTaskId(), task.getTaskType());

        // 执行任务
        return selectedAgent.execute(task);
    }
    
    /**
     * 获取所有可用的Agent
     */
    public List<Agent> getAllAgents() {
        if (agents == null) {
            log.warn("Agents list is null, returning empty list");
            return List.of();
        }
        log.info("Available agents: {}", agents.size());
        return agents;
    }

    /**
     * 推断任务类型
     */
    private String inferTaskType(String userInput) {
        String input = userInput.toLowerCase();

        if (input.contains("表单") || input.contains("form")) {
            return "FORM";
        }
        if (input.contains("流程") || input.contains("工作流") || input.contains("workflow")) {
            return "WORKFLOW";
        }
        if (input.contains("数据") || input.contains("分析") || input.contains("统计") || input.contains("报表")) {
            return "DATA";
        }

        return "GENERAL";
    }

    /**
     * 智能路由 - 找到最合适的Agent
     */
    private Agent findSuitableAgent(AgentTask task) {
        if (agents == null || agents.isEmpty()) {
            log.error("No agents available");
            return null;
        }
        
        log.info("Finding suitable agent for task type: {}", task.getTaskType());
        
        // 优先选择专门处理该任务类型的Agent
        Agent specificAgent = agents.stream()
                .filter(agent -> !"GENERAL".equals(agent.getType()))
                .filter(agent -> agent.canHandle(task))
                .findFirst()
                .orElse(null);

        if (specificAgent != null) {
            return specificAgent;
        }

        // 如果没有专门的Agent，使用通用Agent
        return agents.stream()
                .filter(agent -> "GENERAL".equals(agent.getType()))
                .findFirst()
                .orElse(null);
    }
}