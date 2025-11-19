package top.itangbao.platform.workflow.api.dto; // 包名已修改

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeployDecisionRequest {
    @NotBlank(message = "Deployment name cannot be empty")
    private String deploymentName;

    @NotBlank(message = "DMN XML content cannot be empty")
    private String dmnXml; // DMN 决策定义的 XML 内容

    private String tenantId; // 部署到哪个租户下
}
