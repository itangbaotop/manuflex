package top.itangbao.platform.workflow.api.dto; // 包名已修改

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeployDecisionResponse {
    private String deploymentId;
    private String deploymentName;
    private String decisionDefinitionId;
    private String decisionDefinitionKey;
    private String tenantId;
    private String message;
}
