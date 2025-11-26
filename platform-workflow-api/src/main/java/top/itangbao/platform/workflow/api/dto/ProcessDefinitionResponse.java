package top.itangbao.platform.workflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessDefinitionResponse {
    private String id;
    private String key;
    private String name;
    private int version;
    private String deploymentId;
    private String resource; // BPMN 文件名
    private String tenantId;
    private boolean suspended;
    private LocalDateTime deploymentTime;
}
