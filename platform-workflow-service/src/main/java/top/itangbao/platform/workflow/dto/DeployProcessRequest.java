package top.itangbao.platform.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeployProcessRequest {
    @NotBlank(message = "Deployment name cannot be empty")
    private String deploymentName;

    private String bpmnXml; // BPMN 流程定义的 XML 内容

    @NotBlank(message = "tenantId content cannot be empty")
    private String tenantId; // 部署到哪个租户下 (Camunda 7 支持租户隔离)
}
