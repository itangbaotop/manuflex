package top.itangbao.platform.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService; // 导入 RepositoryService
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.DelegateTask; // 导入 DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.repository.ProcessDefinition; // 导入 ProcessDefinition
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import top.itangbao.platform.workflow.api.dto.ProcessEvent;
import top.itangbao.platform.workflow.api.dto.TaskEvent;
import top.itangbao.platform.workflow.event.WorkflowEventProducer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class GlobalProcessEventListener implements ExecutionListener, TaskListener {

    private final WorkflowEventProducer eventProducer;
    private final RepositoryService repositoryService; // 注入 RepositoryService

    @Autowired
    public GlobalProcessEventListener(WorkflowEventProducer eventProducer, RepositoryService repositoryService) {
        this.eventProducer = eventProducer;
        this.repositoryService = repositoryService;
    }

    // 流程事件监听器
    @Override
    public void notify(DelegateExecution execution) throws Exception {
        log.info("GlobalProcessEventListener: Execution event: {}", execution.getEventName());
        String eventType = execution.getEventName();
        String initiator = getCurrentUsername();

        String processDefinitionKey = null;
        if (execution.getProcessDefinitionId() != null) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(execution.getProcessDefinitionId())
                    .singleResult();
            if (processDefinition != null) {
                processDefinitionKey = processDefinition.getKey();
            }
        }

        ProcessEvent event = ProcessEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PROCESS_" + eventType.toUpperCase())
                .processInstanceId(execution.getProcessInstanceId())
                .processDefinitionId(execution.getProcessDefinitionId())
                .processDefinitionKey(processDefinitionKey) //  使用获取到的 Key
                .businessKey(execution.getProcessBusinessKey())
                .tenantId(execution.getTenantId())
                .activityId(execution.getCurrentActivityId())
                .activityName(execution.getCurrentActivityName())
                .variables(execution.getVariables())
                .initiator(initiator)
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.sendProcessEvent(event);
    }

    // 任务事件监听器
    @Override
    public void notify(DelegateTask delegateTask) { //  修正参数类型为 DelegateTask
        log.info("GlobalProcessEventListener: Task event: {}", delegateTask.getEventName());
        String eventType = delegateTask.getEventName();
        String initiator = getCurrentUsername();

        String processDefinitionKey = null;
        if (delegateTask.getProcessDefinitionId() != null) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(delegateTask.getProcessDefinitionId())
                    .singleResult();
            if (processDefinition != null) {
                processDefinitionKey = processDefinition.getKey();
            }
        }

        TaskEvent event = TaskEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TASK_" + eventType.toUpperCase())
                .taskId(delegateTask.getId())
                .taskName(delegateTask.getName())
                .assignee(delegateTask.getAssignee())
                .owner(delegateTask.getOwner())
                .processInstanceId(delegateTask.getProcessInstanceId())
                .processDefinitionId(delegateTask.getProcessDefinitionId())
                .taskDefinitionKey(processDefinitionKey) //  使用获取到的 Key
                .tenantId(delegateTask.getTenantId())
                .variables(delegateTask.getVariables())
                .initiator(initiator)
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.sendTaskEvent(event);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
