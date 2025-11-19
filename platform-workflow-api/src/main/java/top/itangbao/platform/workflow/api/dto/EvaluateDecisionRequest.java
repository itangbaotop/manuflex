package top.itangbao.platform.workflow.api.dto; // 包名已修改

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
public class EvaluateDecisionRequest {
    @NotBlank(message = "Decision definition key cannot be empty")
    private String decisionDefinitionKey; // 决策定义的 Key (例如: approval-rules)

    private String tenantId; // 决策所属的租户ID

    private Map<String, Object> variables; // 决策输入变量
}
