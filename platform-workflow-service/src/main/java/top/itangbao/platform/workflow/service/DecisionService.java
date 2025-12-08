package top.itangbao.platform.workflow.service;

import top.itangbao.platform.workflow.api.dto.DeployDecisionResponse;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionRequest;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionResponse;

import java.util.List;
import java.util.Map;

public interface DecisionService {
    /**
     * 部署 DMN 决策定义 (通过文件内容)
     * @param deploymentName 部署名称
     * @param fileName DMN 文件名
     * @param dmnBytes DMN XML 文件的字节数组
     * @param tenantId 租户ID
     * @return 部署响应
     */
    DeployDecisionResponse deployDecision(String deploymentName, String fileName, byte[] dmnBytes, String tenantId);

    /**
     * 评估决策
     * @param request 评估决策请求
     * @return 评估决策响应
     */
    EvaluateDecisionResponse evaluateDecision(EvaluateDecisionRequest request);

    /**
     * 获取所有决策定义
     * @param tenantId 租户ID (可选)
     * @return 决策定义列表
     */
    List<Map<String, Object>> getAllDecisionDefinitions(String tenantId);

    /**
     * 根据决策定义 Key 获取所有决策定义
     * @param decisionDefinitionKey 决策定义 Key
     * @param tenantId 租户ID (可选)
     * @return 决策定义列表
     */
    List<Map<String, Object>> getDecisionDefinitions(String decisionDefinitionKey, String tenantId);

    /**
     * 获取决策定义XML
     * @param decisionDefinitionId 决策定义ID
     * @return XML字符串
     */
    String getDecisionDefinitionXml(String decisionDefinitionId);

    /**
     * 删除决策部署
     * @param deploymentId 部署ID
     * @param cascade 是否级联删除
     */
    void deleteDecisionDeployment(String deploymentId, boolean cascade);
}
