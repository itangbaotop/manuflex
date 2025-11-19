package top.itangbao.platform.workflow.dto;

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
public class TaskResponse {
    private String id; // 任务ID
    private String name; // 任务名称
    private String assignee; // 任务执行人
    private String owner; // 任务所有者
    private LocalDateTime createTime;
    private String dueDate; // 到期日期
    private String followUpDate; // 追踪日期
    private String description;
    private String priority;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskDefinitionKey;
    private String tenantId;
    private Map<String, Object> variables; // 任务变量
}
