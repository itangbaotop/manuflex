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
public class ExternalTaskResponse {
    private String id; // 外部任务ID
    private String topic; // 任务主题
    private String workerId; // 任务 Worker ID
    private LocalDateTime lockExpirationTime;
    private String processInstanceId;
    private String processDefinitionId;
    private String activityId;
    private String activityInstanceId;
    private String tenantId;
    private Map<String, Object> variables; // 任务变量
}
