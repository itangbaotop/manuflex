package top.itangbao.platform.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartProcessRequest {
    @NotBlank(message = "Process definition key cannot be empty")
    private String processDefinitionKey; // 流程定义的 Key (例如: my-process)

    private String businessKey; // 业务键，用于关联业务实体

    private String tenantId; // 流程实例所属的租户ID

    private Map<String, Object> variables; // 流程变量
}
