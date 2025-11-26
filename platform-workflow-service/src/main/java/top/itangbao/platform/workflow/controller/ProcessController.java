package top.itangbao.platform.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.workflow.api.dto.DeployProcessRequest;
import top.itangbao.platform.workflow.api.dto.DeployProcessResponse;
import top.itangbao.platform.workflow.api.dto.ProcessDefinitionResponse; // 导入
import top.itangbao.platform.workflow.api.dto.ProcessInstanceMigrationRequest; // 导入
import top.itangbao.platform.workflow.api.dto.ProcessInstanceResponse;
import top.itangbao.platform.workflow.api.dto.StartProcessRequest;
import top.itangbao.platform.workflow.service.ProcessService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class ProcessController {

    private final ProcessService processService;

    @Autowired
    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    /**
     * 部署 BPMN 流程定义 (通过文件上传)
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param deploymentName 部署名称
     * @param tenantId 租户ID
     * @param bpmnFile BPMN XML 文件
     * @return 部署响应
     */
    @PostMapping(value = "/deployments/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('workflow:process:deploy', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") 
    public ResponseEntity<DeployProcessResponse> deployProcessByFile(
            @RequestParam String deploymentName,
            @RequestParam String tenantId,
            @RequestPart("bpmnFile") MultipartFile bpmnFile) throws IOException {
        if (bpmnFile.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DeployProcessResponse response = processService.deployProcess(
                deploymentName,
                bpmnFile.getOriginalFilename(),
                bpmnFile.getBytes(),
                tenantId
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 启动流程实例
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param request 启动流程请求体
     * @return 流程实例响应
     */
    @PostMapping("/process-instances")
    @PreAuthorize("hasAnyAuthority('workflow:process:start', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") 
    public ResponseEntity<ProcessInstanceResponse> startProcessInstance(@Valid @RequestBody StartProcessRequest request) {
        ProcessInstanceResponse response = processService.startProcessInstance(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 根据流程实例 ID 获取流程实例信息
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param processInstanceId 流程实例ID
     * @return 流程实例响应
     */
    @GetMapping("/process-instances/{processInstanceId}")
    @PreAuthorize("hasAnyAuthority('workflow:process:read_instance', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") 
    public ResponseEntity<ProcessInstanceResponse> getProcessInstanceById(@PathVariable String processInstanceId) {
        ProcessInstanceResponse response = processService.getProcessInstanceById(processInstanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据流程定义 Key 获取所有活跃的流程实例
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param processDefinitionKey 流程定义 Key
     * @param tenantId 租户ID (可选)
     * @return 流程实例列表
     */
    @GetMapping("/process-instances/active")
    @PreAuthorize("hasAnyAuthority('workflow:process:read_active_instances', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") 
    public ResponseEntity<List<ProcessInstanceResponse>> getActiveProcessInstances(
            @RequestParam String processDefinitionKey,
            @RequestParam(required = false) String tenantId) {
        List<ProcessInstanceResponse> responses = processService.getActiveProcessInstances(processDefinitionKey, tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 删除流程实例
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param processInstanceId 流程实例ID
     * @param deleteReason 删除原因 (可选)
     * @return 无内容响应
     */
    @DeleteMapping("/process-instances/{processInstanceId}")
    @PreAuthorize("hasAnyAuthority('workflow:process:delete_instance', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") 
    public ResponseEntity<Void> deleteProcessInstance(
            @PathVariable String processInstanceId,
            @RequestParam(required = false, defaultValue = "Deleted by API") String deleteReason) {
        processService.deleteProcessInstance(processInstanceId, deleteReason);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 设置流程变量
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param processInstanceId 流程实例ID
     * @param variables 流程变量
     * @return 成功响应
     */
    @PutMapping("/process-instances/{processInstanceId}/variables")
    @PreAuthorize("hasAnyAuthority('workflow:process:set_variables', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") 
    public ResponseEntity<Void> setProcessVariables(
            @PathVariable String processInstanceId,
            @RequestBody Map<String, Object> variables) {
        processService.setProcessVariables(processInstanceId, variables);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 获取流程变量
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param processInstanceId 流程实例ID
     * @return 流程变量
     */
    @GetMapping("/process-instances/{processInstanceId}/variables")
    @PreAuthorize("hasAnyAuthority('workflow:process:read_variables', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") 
    public ResponseEntity<Map<String, Object>> getProcessVariables(@PathVariable String processInstanceId) {
        Map<String, Object> variables = processService.getProcessVariables(processInstanceId);
        return ResponseEntity.ok(variables);
    }

    /**
     * 查询流程定义
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param key 流程定义 Key (可选)
     * @param tenantId 租户ID (可选)
     * @param latestVersion 是否只查询最新版本
     * @return 流程定义列表
     */
    @GetMapping("/process-definitions")
    @PreAuthorize("hasAnyAuthority('workflow:process:read_definition', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // ⬅️ 新增并细化权限
    public ResponseEntity<List<ProcessDefinitionResponse>> getProcessDefinitions(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "false") boolean latestVersion) {
        List<ProcessDefinitionResponse> definitions = processService.getProcessDefinitions(key, tenantId, latestVersion);
        return ResponseEntity.ok(definitions);
    }

    /**
     * 迁移流程实例
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param request 流程实例迁移请求
     * @return 无内容响应
     */
    @PostMapping("/process-instances/migrate")
    @PreAuthorize("hasAnyAuthority('workflow:process:migrate_instance', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // ⬅️ 新增并细化权限
    public ResponseEntity<Void> migrateProcessInstances(@Valid @RequestBody ProcessInstanceMigrationRequest request) {
        processService.migrateProcessInstances(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
