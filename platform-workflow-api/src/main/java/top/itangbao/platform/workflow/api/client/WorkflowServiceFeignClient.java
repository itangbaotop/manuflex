package top.itangbao.platform.workflow.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType; // 导入 MediaType
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 导入 MultipartFile
import top.itangbao.platform.workflow.api.dto.ClaimTaskRequest; // 导入 DTOs
import top.itangbao.platform.workflow.api.dto.CompleteTaskRequest;
import top.itangbao.platform.workflow.api.dto.DeployProcessRequest;
import top.itangbao.platform.workflow.api.dto.DeployProcessResponse;
import top.itangbao.platform.workflow.api.dto.ExternalTaskResponse;
import top.itangbao.platform.workflow.api.dto.ProcessInstanceResponse;
import top.itangbao.platform.workflow.api.dto.StartProcessRequest;
import top.itangbao.platform.workflow.api.dto.TaskResponse;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端，用于调用 Workflow Service
 */
@FeignClient(name = "workflow-service", url = "${workflow.service.url}")
public interface WorkflowServiceFeignClient {

    // --- 流程管理 API ---
    @PostMapping(value = "/api/workflow/deployments/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DeployProcessResponse deployProcessByFile(
            @RequestPart("deploymentName") String deploymentName,
            @RequestPart("tenantId") String tenantId,
            @RequestPart("bpmnFile") MultipartFile bpmnFile);

    @PostMapping("/api/workflow/process-instances")
    ProcessInstanceResponse startProcessInstance(@RequestBody StartProcessRequest request);

    @GetMapping("/api/workflow/process-instances/{processInstanceId}")
    ProcessInstanceResponse getProcessInstanceById(@PathVariable("processInstanceId") String processInstanceId);

    @GetMapping("/api/workflow/process-instances/active")
    List<ProcessInstanceResponse> getActiveProcessInstances(
            @RequestParam("processDefinitionKey") String processDefinitionKey,
            @RequestParam(value = "tenantId", required = false) String tenantId);

    @DeleteMapping("/api/workflow/process-instances/{processInstanceId}")
    void deleteProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam(value = "deleteReason", required = false, defaultValue = "Deleted by API") String deleteReason);

    @PutMapping("/api/workflow/process-instances/{processInstanceId}/variables")
    void setProcessVariables(
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestBody Map<String, Object> variables);

    @GetMapping("/api/workflow/process-instances/{processInstanceId}/variables")
    Map<String, Object> getProcessVariables(@PathVariable("processInstanceId") String processInstanceId);


    // --- 任务管理 API ---
    @GetMapping("/api/workflow/tasks/{taskId}")
    TaskResponse getTaskById(@PathVariable("taskId") String taskId);

    @GetMapping("/api/workflow/tasks/by-process-instance/{processInstanceId}")
    List<TaskResponse> getTasksByProcessInstanceId(
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetMapping("/api/workflow/tasks/by-assignee/{assignee}")
    List<TaskResponse> getTasksByAssignee(
            @PathVariable("assignee") String assignee,
            @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetMapping("/api/workflow/tasks/unassigned")
    List<TaskResponse> getUnassignedTasks(@RequestParam(value = "tenantId", required = false) String tenantId);

    @PostMapping("/api/workflow/tasks/claim")
    void claimTask(@RequestBody ClaimTaskRequest request);

    @PostMapping("/api/workflow/tasks/complete")
    void completeTask(@RequestBody CompleteTaskRequest request);

    @PostMapping("/api/workflow/tasks/unclaim/{taskId}")
    void unclaimTask(@PathVariable("taskId") String taskId);

    @GetMapping("/api/workflow/tasks/external")
    List<ExternalTaskResponse> getExternalTasks(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "tenantId", required = false) String tenantId);
}
