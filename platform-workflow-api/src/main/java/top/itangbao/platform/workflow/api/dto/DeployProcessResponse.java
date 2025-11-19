package top.itangbao.platform.workflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeployProcessResponse {
    private String deploymentId;
    private String deploymentName;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String tenantId;
    private String message;
}
