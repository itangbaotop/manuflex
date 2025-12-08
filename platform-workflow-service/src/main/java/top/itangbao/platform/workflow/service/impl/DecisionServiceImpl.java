package top.itangbao.platform.workflow.service.impl;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.dmn.DecisionEvaluationBuilder;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder; // 导入构建器
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.workflow.api.dto.DeployDecisionResponse;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionRequest;
import top.itangbao.platform.workflow.api.dto.EvaluateDecisionResponse;
import top.itangbao.platform.workflow.service.DecisionService;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DecisionServiceImpl implements DecisionService {

    private final RepositoryService repositoryService;
    // 使用全限定名避免与自定义 DecisionService 冲突
    private final org.camunda.bpm.engine.DecisionService camundaDecisionService;

    @Autowired
    public DecisionServiceImpl(RepositoryService repositoryService,
                               org.camunda.bpm.engine.DecisionService camundaDecisionService) {
        this.repositoryService = repositoryService;
        this.camundaDecisionService = camundaDecisionService;
    }

    @Override
    @Transactional
    public DeployDecisionResponse deployDecision(String deploymentName, String fileName, byte[] dmnBytes, String tenantId) {
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                .name(deploymentName)
                .addInputStream(fileName, new ByteArrayInputStream(dmnBytes));

        // 只有当 tenantId 不为空时才设置
        if (tenantId != null && !tenantId.isEmpty()) {
            deploymentBuilder.tenantId(tenantId);
        }

        Deployment deployment = deploymentBuilder.deploy();

        DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        // 增加空值判断，防止部署非 DMN 文件时报错
        String defId = (decisionDefinition != null) ? decisionDefinition.getId() : null;
        String defKey = (decisionDefinition != null) ? decisionDefinition.getKey() : null;

        return DeployDecisionResponse.builder()
                .deploymentId(deployment.getId())
                .deploymentName(deployment.getName())
                .decisionDefinitionId(defId)
                .decisionDefinitionKey(defKey)
                .tenantId(deployment.getTenantId())
                .message("Decision deployed successfully.")
                .build();
    }

    @Override
    @Transactional
    public EvaluateDecisionResponse evaluateDecision(EvaluateDecisionRequest request) {
        DecisionEvaluationBuilder evaluationBuilder = camundaDecisionService
                .evaluateDecisionTableByKey(request.getDecisionDefinitionKey());

        if (request.getTenantId() != null && !request.getTenantId().isEmpty()) {
            evaluationBuilder.decisionDefinitionTenantId(request.getTenantId());
        }

        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            VariableMap variables = Variables.createVariables();
            request.getVariables().forEach(variables::putValue);
            evaluationBuilder.variables(variables);
        }

        // 执行评估
        DmnDecisionTableResult dmnDecisionTableResult = evaluationBuilder.evaluate();

        List<Map<String, Object>> results = dmnDecisionTableResult.getResultList().stream()
                .map(result -> (Map<String, Object>) result)
                .collect(Collectors.toList());

        return EvaluateDecisionResponse.builder()
                .decisionDefinitionKey(request.getDecisionDefinitionKey())
                .tenantId(request.getTenantId())
                .results(results)
                .build();
    }

    @Override
    public List<Map<String, Object>> getAllDecisionDefinitions(String tenantId) {
        var query = repositoryService.createDecisionDefinitionQuery();

        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }

        query.orderByDecisionDefinitionVersion().desc();

        return query.list().stream()
                .map(decisionDefinition -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", decisionDefinition.getId());
                    map.put("key", decisionDefinition.getKey());
                    map.put("name", decisionDefinition.getName());
                    map.put("version", decisionDefinition.getVersion());
                    map.put("deploymentId", decisionDefinition.getDeploymentId());
                    map.put("tenantId", decisionDefinition.getTenantId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDecisionDefinitions(String decisionDefinitionKey, String tenantId) {
        var query = repositoryService.createDecisionDefinitionQuery();

        if (decisionDefinitionKey != null && !decisionDefinitionKey.isEmpty()) {
            query.decisionDefinitionKey(decisionDefinitionKey);
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }

        // 按版本降序，方便查看最新版
        query.orderByDecisionDefinitionVersion().desc();

        return query.list().stream()
                .map(decisionDefinition -> {
                    // [修复] 使用 HashMap 替代 Map.of，允许 value 为 null (如 tenantId)
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", decisionDefinition.getId());
                    map.put("key", decisionDefinition.getKey());
                    map.put("name", decisionDefinition.getName());
                    map.put("version", decisionDefinition.getVersion());
                    map.put("deploymentId", decisionDefinition.getDeploymentId());
                    map.put("tenantId", decisionDefinition.getTenantId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getDecisionDefinitionXml(String decisionDefinitionId) {
        try {
            DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
                    .decisionDefinitionId(decisionDefinitionId)
                    .singleResult();
            
            if (decisionDefinition == null) {
                throw new RuntimeException("Decision definition not found with ID: " + decisionDefinitionId);
            }
            
            return new String(repositoryService.getDecisionModel(decisionDefinitionId).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get decision definition XML", e);
        }
    }

    @Override
    @Transactional
    public void deleteDecisionDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }
}