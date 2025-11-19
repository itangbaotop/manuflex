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
public class ProcessInstanceResponse {
    private String id; // 流程实例ID
    private String processDefinitionId;
    private String businessKey;
    private String tenantId;
    private boolean ended;
    private LocalDateTime startTime;
    private Map<String, Object> currentVariables; // 当前流程变量
}
