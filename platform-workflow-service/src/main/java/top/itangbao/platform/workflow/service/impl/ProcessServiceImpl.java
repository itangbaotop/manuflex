package top.itangbao.platform.workflow.service.impl;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanExecutionBuilder;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.workflow.api.dto.*;
import top.itangbao.platform.workflow.service.ProcessService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProcessServiceImpl implements ProcessService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    @Autowired
    public ProcessServiceImpl(RepositoryService repositoryService,
                              RuntimeService runtimeService,
                              HistoryService historyService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    @Override
    @Transactional
    public DeployProcessResponse deployProcess(DeployProcessRequest request) {
        Deployment deployment = repositoryService.createDeployment()
                .name(request.getDeploymentName())
                .addInputStream(request.getDeploymentName() + ".bpmn", new ByteArrayInputStream(request.getBpmnXml().getBytes()))
                .tenantId(request.getTenantId())
                .deploy();

        org.camunda.bpm.engine.repository.ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        return DeployProcessResponse.builder()
                .deploymentId(deployment.getId())
                .deploymentName(deployment.getName())
                .processDefinitionId(processDefinition.getId())
                .processDefinitionKey(processDefinition.getKey())
                .tenantId(deployment.getTenantId())
                .message("Process deployed successfully.")
                .build();
    }

    @Override
    @Transactional
    public DeployProcessResponse deployProcess(String deploymentName, String fileName, byte[] bpmnBytes, String tenantId) { // 修改方法签名
        Deployment deployment = repositoryService.createDeployment()
                .name(deploymentName)
                .addInputStream(fileName, new ByteArrayInputStream(bpmnBytes)) // 使用文件名和字节数组
                .tenantId(tenantId)
                .deploy();

        org.camunda.bpm.engine.repository.ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        return DeployProcessResponse.builder()
                .deploymentId(deployment.getId())
                .deploymentName(deployment.getName())
                .processDefinitionId(processDefinition.getId())
                .processDefinitionKey(processDefinition.getKey())
                .tenantId(deployment.getTenantId())
                .message("Process deployed successfully.")
                .build();
    }

    @Override
    @Transactional
    public ProcessInstanceResponse startProcessInstance(StartProcessRequest request) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                request.getProcessDefinitionKey(),
                request.getBusinessKey(),
                request.getVariables()
        );

        Map<String, Object> currentVariables = runtimeService.getVariables(processInstance.getId());

        return ProcessInstanceResponse.builder()
                .id(processInstance.getId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .businessKey(processInstance.getBusinessKey())
                .tenantId(processInstance.getTenantId())
                .ended(false) // 新启动的流程实例，默认未结束
                .startTime(LocalDateTime.now())
                .currentVariables(currentVariables)
                .build();
    }

    @Override
    public ProcessInstanceResponse getProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicProcessInstance == null) {
            throw new ResourceNotFoundException("Process instance not found with ID: " + processInstanceId);
        }

        // 判断流程实例是否结束，使用 getEndTime() != null
        boolean ended = historicProcessInstance.getEndTime() != null;
        Map<String, Object> currentVariables;

        if (!ended) { // 如果是活跃实例
            try {
                currentVariables = runtimeService.getVariables(processInstanceId);
            } catch (Exception e) {
                // 如果 runtimeService 找不到变量 (例如，在某些情况下，即使流程活跃，变量也可能不在 runtime 层面)
                currentVariables = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .list().stream()
                        .collect(Collectors.toMap(
                                historicVariableInstance -> historicVariableInstance.getName(),
                                historicVariableInstance -> historicVariableInstance.getValue()
                        ));
            }
        } else { // 如果是已结束实例
            currentVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list().stream()
                    .collect(Collectors.toMap(
                            historicVariableInstance -> historicVariableInstance.getName(),
                            historicVariableInstance -> historicVariableInstance.getValue()
                    ));
        }

        return ProcessInstanceResponse.builder()
                .id(historicProcessInstance.getId())
                .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                .businessKey(historicProcessInstance.getBusinessKey())
                .tenantId(historicProcessInstance.getTenantId())
                .ended(ended) // 使用正确的 ended 状态
                .startTime(historicProcessInstance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .currentVariables(currentVariables)
                .build();
    }

    @Override
    public List<ProcessInstanceResponse> getActiveProcessInstances(String processDefinitionKey, String tenantId) {
        var query = runtimeService.createProcessInstanceQuery().active();
        
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        
        return query.list().stream()
                .map(processInstance -> {
                    Map<String, Object> currentVariables = runtimeService.getVariables(processInstance.getId());
                    return ProcessInstanceResponse.builder()
                            .id(processInstance.getId())
                            .processDefinitionId(processInstance.getProcessDefinitionId())
                            .businessKey(processInstance.getBusinessKey())
                            .tenantId(processInstance.getTenantId())
                            .ended(false)
                            .startTime(LocalDateTime.now())
                            .currentVariables(currentVariables)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
    }

    @Override
    @Transactional
    public void setProcessVariables(String processInstanceId, Map<String, Object> variables) {
        runtimeService.setVariables(processInstanceId, variables);
    }

    @Override
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    @Override
    public List<ProcessDefinitionResponse> getProcessDefinitions(String key, String tenantId, boolean latestVersion) {
        var query = repositoryService.createProcessDefinitionQuery();
        if (key != null && !key.isEmpty()) {
            query.processDefinitionKey(key);
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        if (latestVersion) {
            query.latestVersion();
        }

        return query.list().stream()
                .map(this::convertToProcessDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void migrateProcessInstances(ProcessInstanceMigrationRequest request) {
        // 1. 创建迁移计划
        MigrationPlan migrationPlan = runtimeService.createMigrationPlan(request.getSourceProcessDefinitionId(), request.getTargetProcessDefinitionId())
                // 默认情况下，Camunda 会尝试自动映射相同 ID 的活动
                // 如果活动 ID 发生变化，需要手动指定映射规则
                // .mapEqualActivities()
                // .mapActivities("oldActivityId", "newActivityId")
                .build();

        // 2. 执行迁移
        MigrationPlanExecutionBuilder migrationExecution = runtimeService.newMigration(migrationPlan);

        if (request.getProcessInstanceIds() != null && !request.getProcessInstanceIds().isEmpty()) {
            migrationExecution.processInstanceIds(request.getProcessInstanceIds());
        } else {
            // 如果没有指定实例ID，则默认迁移所有活跃实例
            migrationExecution.processInstanceQuery(
                    runtimeService.createProcessInstanceQuery()
                            .processDefinitionId(request.getSourceProcessDefinitionId())
            );
        }
        migrationExecution.execute();
    }

    // 辅助方法：将 ProcessDefinition 实体转换为 ProcessDefinitionResponse DTO
    private ProcessDefinitionResponse convertToProcessDefinitionResponse(ProcessDefinition processDefinition) {
        return ProcessDefinitionResponse.builder()
                .id(processDefinition.getId())
                .key(processDefinition.getKey())
                .name(processDefinition.getName())
                .version(processDefinition.getVersion())
                .deploymentId(processDefinition.getDeploymentId())
                .resource(processDefinition.getResourceName())
                .tenantId(processDefinition.getTenantId())
                .suspended(processDefinition.isSuspended())
                // 部署时间需要从历史服务获取部署信息
                .deploymentTime(getDeploymentTime(processDefinition.getDeploymentId()))
                .build();
    }

    // 辅助方法：获取部署时间
    private LocalDateTime getDeploymentTime(String deploymentId) {
        if (deploymentId == null) {
            return null;
        }
        try {
            Deployment deployment = repositoryService.createDeploymentQuery()
                    .deploymentId(deploymentId)
                    .singleResult();
            if (deployment != null && deployment.getDeploymentTime() != null) {
                return deployment.getDeploymentTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        } catch (Exception e) {
            // 部署可能已删除或发生其他错误
            // logger.warn("Failed to get deployment time for deployment ID {}: {}", deploymentId, e.getMessage());
        }
        return null;
    }

    @Override
    public String getProcessDefinitionXml(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            
            if (processDefinition == null) {
                throw new ResourceNotFoundException("Process definition not found with ID: " + processDefinitionId);
            }
            
            return new String(repositoryService.getProcessModel(processDefinitionId).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get process definition XML", e);
        }
    }

    @Override
    @Transactional
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    @Override
    public List<String> getActiveActivities(String processInstanceId) {
        return runtimeService.getActiveActivityIds(processInstanceId);
    }

    @Override
    public List<ProcessInstanceResponse> getHistoricProcessInstances(String processDefinitionKey, String tenantId) {
        var query = historyService.createHistoricProcessInstanceQuery();
        
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            query.tenantIdIn(tenantId);
        }
        
        return query.orderByProcessInstanceStartTime().desc().list().stream()
                .map(historicProcessInstance -> {
                    Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(historicProcessInstance.getId())
                            .list().stream()
                            .collect(Collectors.toMap(
                                    hvi -> hvi.getName(),
                                    hvi -> hvi.getValue()
                            ));
                    
                    return ProcessInstanceResponse.builder()
                            .id(historicProcessInstance.getId())
                            .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                            .businessKey(historicProcessInstance.getBusinessKey())
                            .tenantId(historicProcessInstance.getTenantId())
                            .ended(historicProcessInstance.getEndTime() != null)
                            .startTime(historicProcessInstance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .currentVariables(variables)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
