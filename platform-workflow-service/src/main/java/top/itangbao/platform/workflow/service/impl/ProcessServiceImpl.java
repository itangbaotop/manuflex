package top.itangbao.platform.workflow.service.impl;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.workflow.dto.DeployProcessRequest;
import top.itangbao.platform.workflow.dto.DeployProcessResponse;
import top.itangbao.platform.workflow.dto.ProcessInstanceResponse;
import top.itangbao.platform.workflow.dto.StartProcessRequest;
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
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .tenantIdIn(tenantId)
                .list()
                .stream()
                .map(processInstance -> {
                    Map<String, Object> currentVariables = runtimeService.getVariables(processInstance.getId());
                    return ProcessInstanceResponse.builder()
                            .id(processInstance.getId())
                            .processDefinitionId(processInstance.getProcessDefinitionId())
                            .businessKey(processInstance.getBusinessKey())
                            .tenantId(processInstance.getTenantId())
                            .ended(false) // 活跃实例，默认未结束
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
}
