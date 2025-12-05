package top.itangbao.platform.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.workflow.api.dto.DeployDecisionRequest; // 引入 API 模块的 DTO
import top.itangbao.platform.workflow.api.dto.DeployDecisionResponse;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionRequest;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionResponse;
import top.itangbao.platform.workflow.service.DecisionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/decision") // 定义决策管理的基础路径
public class DecisionController {

    private final DecisionService decisionService;

    @Autowired
    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    /**
     * 部署 DMN 决策定义 (通过文件上传)
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色的用户才能访问
     * @param deploymentName 部署名称
     * @param tenantId 租户ID
     * @param dmnFile DMN XML 文件
     * @return 部署响应
     */
    @PostMapping(value = "/deployments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化权限
    public ResponseEntity<DeployDecisionResponse> deployDecisionByFile(
            @RequestParam String deploymentName,
            @RequestParam String tenantId,
            @RequestPart("dmnFile") MultipartFile dmnFile) throws IOException {
        if (dmnFile.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DeployDecisionResponse response = decisionService.deployDecision(
                deploymentName,
                dmnFile.getOriginalFilename(),
                dmnFile.getBytes(),
                tenantId
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 评估决策
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param request 评估决策请求
     * @return 评估决策响应
     */
    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限
    public ResponseEntity<EvaluateDecisionResponse> evaluateDecision(@Valid @RequestBody EvaluateDecisionRequest request) {
        EvaluateDecisionResponse response = decisionService.evaluateDecision(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据决策定义 Key 获取所有决策定义
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param decisionDefinitionKey 决策定义 Key
     * @param tenantId 租户ID (可选)
     * @return 决策定义列表
     */
    @GetMapping("/definitions/{decisionDefinitionKey}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化权限
    public ResponseEntity<List<Map<String, Object>>> getDecisionDefinitions(
            @PathVariable String decisionDefinitionKey,
            @RequestParam(required = false) String tenantId) {
        List<Map<String, Object>> definitions = decisionService.getDecisionDefinitions(decisionDefinitionKey, tenantId);
        return ResponseEntity.ok(definitions);
    }

    /**
     * 删除决策部署
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色的用户才能访问
     * @param deploymentId 部署ID
     * @param cascade 是否级联删除 (是否删除所有相关历史)
     * @return 无内容响应
     */
    @DeleteMapping("/deployments/{deploymentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化权限
    public ResponseEntity<Void> deleteDecisionDeployment(
            @PathVariable String deploymentId,
            @RequestParam(defaultValue = "false") boolean cascade) {
        decisionService.deleteDecisionDeployment(deploymentId, cascade);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
