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
public class ProcessEvent {
    private String eventId; // 事件唯一ID
    private String eventType; // 事件类型 (例如: PROCESS_STARTED, PROCESS_ENDED, ACTIVITY_STARTED)
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private String tenantId;
    private String activityId; // 如果是活动事件
    private String activityName;
    private Map<String, Object> variables; // 流程变量快照
    private String initiator; // 触发事件的用户
    private LocalDateTime timestamp;
}
