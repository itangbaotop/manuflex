package top.itangbao.platform.workflow.service;

import top.itangbao.platform.workflow.api.dto.ClaimTaskRequest;
import top.itangbao.platform.workflow.api.dto.CompleteTaskRequest;
import top.itangbao.platform.workflow.api.dto.ExternalTaskResponse;
import top.itangbao.platform.workflow.api.dto.TaskResponse;

import java.util.List;

public interface TaskService {
    /**
     * 根据任务 ID 获取用户任务信息
     * @param taskId 任务ID
     * @return 任务响应
     */
    TaskResponse getTaskById(String taskId);

    /**
     * 根据流程实例 ID 获取所有用户任务
     * @param processInstanceId 流程实例ID
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    List<TaskResponse> getTasksByProcessInstanceId(String processInstanceId, String tenantId);

    /**
     * 获取分配给指定执行人的任务列表
     * @param assignee 任务执行人
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    List<TaskResponse> getTasksByAssignee(String assignee, String tenantId);

    /**
     * 获取未分配的任务列表
     * @param tenantId 租户ID (可选)
     * @return 任务列表
     */
    List<TaskResponse> getUnassignedTasks(String tenantId);

    /**
     * 认领任务
     * @param request 认领请求
     */
    void claimTask(ClaimTaskRequest request);

    /**
     * 完成任务
     * @param request 完成任务请求
     */
    void completeTask(CompleteTaskRequest request);

    /**
     * 解除任务认领
     * @param taskId 任务ID
     */
    void unclaimTask(String taskId);

    // --- 外部任务相关方法 (External Task) ---
    // 这些方法通常由外部 Worker 调用，而不是直接通过 REST API 暴露给前端
    // 但为了管理和调试，可以提供一些查询接口

    /**
     * 获取所有活跃的外部任务
     * @param topic 任务主题 (可选)
     * @param tenantId 租户ID (可选)
     * @return 外部任务列表
     */
    List<ExternalTaskResponse> getExternalTasks(String topic, String tenantId);

    // 更多外部任务操作：锁定、完成、失败等，通常由 External Task Worker 客户端库处理
}
