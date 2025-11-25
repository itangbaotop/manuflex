package top.itangbao.platform.workflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEvent {
    private String eventId; // 事件唯一ID
    private String eventType; // 事件类型 (例如: TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED)
    private String taskId;
    private String taskName;
    private String assignee;
    private String owner;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskDefinitionKey;
    private String tenantId;
    private Map<String, Object> variables; // 任务变量快照
    private String initiator; // 触发事件的用户
    private LocalDateTime timestamp;
}
