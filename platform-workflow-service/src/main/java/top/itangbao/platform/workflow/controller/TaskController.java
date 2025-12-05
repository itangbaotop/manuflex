package top.itangbao.platform.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.workflow.api.dto.ClaimTaskRequest;
import top.itangbao.platform.workflow.api.dto.CompleteTaskRequest;
import top.itangbao.platform.workflow.api.dto.ExternalTaskResponse;
import top.itangbao.platform.workflow.api.dto.TaskResponse;
import top.itangbao.platform.workflow.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/tasks") // 定义任务管理的基础路径
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 根据任务 ID 获取用户任务信息
     * @param taskId 任务ID
     * @return 任务响应
     */
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限，例如只有任务的 assignee/owner/admin 才能看
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String taskId) {
        TaskResponse response = taskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据流程实例 ID 获取所有用户任务
     * @param processInstanceId 流程实例ID
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    @GetMapping("/by-process-instance/{processInstanceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<TaskResponse>> getTasksByProcessInstanceId(
            @PathVariable String processInstanceId,
            @RequestParam(required = false) String tenantId) {
        List<TaskResponse> responses = taskService.getTasksByProcessInstanceId(processInstanceId, tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 获取分配给指定执行人的任务列表
     * @param assignee 任务执行人
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    @GetMapping("/by-assignee/{assignee}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限，例如只有 assignee 自身和 admin 才能看
    public ResponseEntity<List<TaskResponse>> getTasksByAssignee(
            @PathVariable String assignee,
            @RequestParam(required = false) String tenantId) {
        List<TaskResponse> responses = taskService.getTasksByAssignee(assignee, tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 获取未分配的任务列表
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色的用户才能访问
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化权限，例如只有管理员或特定角色才能看所有未分配任务
    public ResponseEntity<List<TaskResponse>> getUnassignedTasks(@RequestParam(required = false) String tenantId) {
        List<TaskResponse> responses = taskService.getUnassignedTasks(tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 认领任务
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param request 认领请求
     * @return 无内容响应
     */
    @PostMapping("/claim")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限，例如只有特定组的用户才能认领
    public ResponseEntity<Void> claimTask(@Valid @RequestBody ClaimTaskRequest request) {
        taskService.claimTask(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 完成任务
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param request 完成任务请求
     * @return 无内容响应
     */
    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限，例如只有任务的 assignee 才能完成
    public ResponseEntity<Void> completeTask(@Valid @RequestBody CompleteTaskRequest request) {
        taskService.completeTask(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 解除任务认领
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param taskId 任务ID
     * @return 无内容响应
     */
    @PostMapping("/unclaim/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')") // TODO: 细化权限，例如只有任务的 assignee 或 admin 才能解除认领
    public ResponseEntity<Void> unclaimTask(@PathVariable String taskId) {
        taskService.unclaimTask(taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 获取所有活跃的外部任务
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色的用户才能访问
     * @param topic 任务主题 (可选)
     * @param tenantId 租户ID (可选)
     * @return 外部任务列表
     */
    @GetMapping("/external")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // 外部任务通常由后台 Worker 处理，这里提供给管理员查询
    public ResponseEntity<List<ExternalTaskResponse>> getExternalTasks(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String tenantId) {
        List<ExternalTaskResponse> responses = taskService.getExternalTasks(topic, tenantId);
        return ResponseEntity.ok(responses);
    }
}
