package top.itangbao.platform.workflow.service.impl;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.workflow.dto.ClaimTaskRequest;
import top.itangbao.platform.workflow.dto.CompleteTaskRequest;
import top.itangbao.platform.workflow.dto.ExternalTaskResponse;
import top.itangbao.platform.workflow.dto.TaskResponse;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements top.itangbao.platform.workflow.service.TaskService {

    private final TaskService camundaTaskService;
    private final ExternalTaskService camundaExternalTaskService;
    private final RuntimeService runtimeService; // [新增]

    @Autowired
    public TaskServiceImpl(TaskService camundaTaskService,
                           ExternalTaskService camundaExternalTaskService,
                           RuntimeService runtimeService) {
        this.camundaTaskService = camundaTaskService;
        this.camundaExternalTaskService = camundaExternalTaskService;
        this.runtimeService = runtimeService;
    }

    @Override
    public TaskResponse getTaskById(String taskId) {
        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with ID: " + taskId);
        }
        return convertToTaskResponse(task);
    }

    @Override
    public List<TaskResponse> getTasksByProcessInstanceId(String processInstanceId, String tenantId) {
        var query = camundaTaskService.createTaskQuery()
                .processInstanceId(processInstanceId);
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        return query.list().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksByAssignee(String assignee, String tenantId) {
        var query = camundaTaskService.createTaskQuery()
                .taskAssignee(assignee);
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        return query.list().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getUnassignedTasks(String tenantId) {
        var query = camundaTaskService.createTaskQuery()
                .taskUnassigned();
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        return query.list().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void claimTask(ClaimTaskRequest request) {
        camundaTaskService.claim(request.getTaskId(), request.getAssignee());
    }

    @Override
    @Transactional
    public void completeTask(CompleteTaskRequest request) {
        camundaTaskService.complete(request.getTaskId(), request.getVariables());
    }

    @Override
    @Transactional
    public void unclaimTask(String taskId) {
        camundaTaskService.setAssignee(taskId, null);
    }

    @Override
    public List<ExternalTaskResponse> getExternalTasks(String topic, String tenantId) {
        var query = camundaExternalTaskService.createExternalTaskQuery();
        if (topic != null && !topic.isEmpty()) {
            query.topicName(topic);
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        return query.list().stream()
                .map(this::convertToExternalTaskResponse)
                .collect(Collectors.toList());
    }

    // 辅助方法：将 Camunda Task 实体转换为 TaskResponse DTO
    private TaskResponse convertToTaskResponse(Task task) {
        Map<String, Object> variables = camundaTaskService.getVariables(task.getId());
        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime() != null ? task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .dueDate(task.getDueDate() != null ? task.getDueDate().toString() : null)
                .followUpDate(task.getFollowUpDate() != null ? task.getFollowUpDate().toString() : null)
                .description(task.getDescription())
                .priority(String.valueOf(task.getPriority()))
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .tenantId(task.getTenantId())
                .variables(variables)
                .build();
    }

    // 辅助方法：将 Camunda ExternalTask 实体转换为 ExternalTaskResponse DTO
    private ExternalTaskResponse convertToExternalTaskResponse(ExternalTask externalTask) {
        // [修正] ExternalTaskService 无法直接获取变量，需要通过 Execution 获取
        // 如果外部任务关联了 Execution，则从 RuntimeService 获取变量
        Map<String, Object> variables = null;
        if (externalTask.getExecutionId() != null) {
            variables = runtimeService.getVariables(externalTask.getExecutionId());
        }

        return ExternalTaskResponse.builder()
                .id(externalTask.getId())
                .topic(externalTask.getTopicName())
                .workerId(externalTask.getWorkerId())
                .lockExpirationTime(externalTask.getLockExpirationTime() != null ? externalTask.getLockExpirationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .processInstanceId(externalTask.getProcessInstanceId())
                .processDefinitionId(externalTask.getProcessDefinitionId())
                .activityId(externalTask.getActivityId())
                .activityInstanceId(externalTask.getActivityInstanceId())
                .tenantId(externalTask.getTenantId())
                .variables(variables)
                .build();
    }
}