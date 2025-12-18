package top.itangbao.platform.agent.aiagent.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import top.itangbao.platform.agent.aiagent.tools.SchemaTools;
import top.itangbao.platform.agent.dto.AgentResult;
import top.itangbao.platform.agent.aiagent.service.AgentService;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.common.util.SecurityUtils;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;

import java.util.ArrayList;
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

    private final MetadataServiceFeignClient metadataServiceFeignClient;

    private final SchemaTools schemaTools;

    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> test() {
//        MetadataSchemaDTO schemaById = metadataServiceFeignClient.getSchemaById(3L);

        String formName = "customer";
        String description = "客户表单";
        String tenantId = "001";
        String fieldsJson = "[{\"name\": \"name\", \"label\": \"姓名\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"phone\", \"label\": \"电话\", \"type\": \"STRING\", \"required\": true}]";

        String formSchema = schemaTools.createFormSchema(formName, description, tenantId, "4", fieldsJson);

        return ResponseEntity.ok("test");
    }
    
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

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<String> streamTask(@RequestBody Map<String, String> request) {
        CustomUserDetails loginUser = SecurityUtils.getLoginUser();
        String tenantId = null;
        String userId = null;
        if (loginUser != null) {
            tenantId = loginUser.getTenantId();
            userId = loginUser.getUsername();
        }

        String userInput = request.get("input");

        return agentService.executeTaskStream(userInput, tenantId, userId).map(chunk -> chunk.startsWith("data:") ? chunk.substring(5).trim() : chunk);
    }

    /**
     * 获取当前 AI 助理具备的能力列表
     * (重写此方法以适配新架构)
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentInfo>> listAgents() {
        // 在新架构中，Agent 是统一的，这里返回它具备的“技能”作为虚拟 Agent 展示
        List<AgentInfo> capabilities = new ArrayList<>();

        capabilities.add(new AgentInfo(
                "FORM",
                "表单设计助手",
                "支持通过对话自动创建数据模型和表单界面"
        ));

        capabilities.add(new AgentInfo(
                "WORKFLOW",
                "流程管理助手",
                "支持查询现有流程、发起新流程和任务处理"
        ));

        capabilities.add(new AgentInfo(
                "GENERAL", // 前端会映射为紫色，对应知识库/通用问答
                "企业知识助手",
                "基于企业知识库回答业务规则和操作指南"
        ));

        capabilities.add(new AgentInfo(
                "DATA",
                "数据分析助手",
                "支持自然语言查询业务数据和简单统计"
        ));

        return ResponseEntity.ok(capabilities);
    }

    /**
     * Agent 信息DTO
     */
    public record AgentInfo(String type, String name, String description) {}
}