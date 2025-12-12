package top.itangbao.platform.agent.aiagent.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.aiagent.service.AgentService;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.common.util.SecurityUtils;

import java.util.List;
import java.util.Map;

/**
 * Agent API 控制器
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    
    /**
     * 执行Agent任务
     */
    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentResult> executeTask(@RequestBody Map<String, String> request) {
        try {
            System.out.println("AgentController.executeTask() called");
            String userInput = request.get("input");
            CustomUserDetails loginUser = SecurityUtils.getLoginUser();
            String tenantId = loginUser.getTenantId();
            String userId = loginUser.getUsername();
            System.out.println("User: " + userId + ", Tenant: " + tenantId);

            AgentResult result = agentService.executeTask(userInput, tenantId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            AgentResult errorResult = AgentResult.failure("认证失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 获取所有可用的Agent
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentInfo>> listAgents() {
        System.out.println("AgentController.listAgents() called");
        List<Agent> agents = agentService.getAllAgents();
        List<AgentInfo> agentInfos = agents.stream()
                .map(agent -> new AgentInfo(
                        agent.getType(),
                        agent.getName(),
                        agent.getDescription()
                ))
                .toList();
        
        return ResponseEntity.ok(agentInfos);
    }
    

    /**
     * Agent 信息DTO
     */
    public record AgentInfo(String type, String name, String description) {}
}