package top.itangbao.platform.workflow.api.dto; // 包名已修改

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluateDecisionResponse {
    private String decisionDefinitionId;
    private String decisionDefinitionKey;
    private String tenantId;
    private List<Map<String, Object>> results; // 决策评估结果
}
