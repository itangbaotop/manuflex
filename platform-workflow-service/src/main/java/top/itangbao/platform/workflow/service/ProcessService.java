package top.itangbao.platform.workflow.service;

import top.itangbao.platform.workflow.api.dto.*;

import java.util.List;
import java.util.Map;

public interface ProcessService {
    /**
     * 部署 BPMN 流程定义
     * @param request 部署请求体
     * @return 部署响应
     */
    DeployProcessResponse deployProcess(DeployProcessRequest request);

    /**
     * 部署 BPMN 流程定义 (通过文件内容)
     * @param deploymentName 部署名称
     * @param fileName BPMN 文件名
     * @param bpmnBytes BPMN XML 文件的字节数组
     * @param tenantId 租户ID
     * @return 部署响应
     */
    DeployProcessResponse deployProcess(String deploymentName, String fileName, byte[] bpmnBytes, String tenantId);


    /**
     * 启动流程实例
     * @param request 启动流程请求体
     * @return 流程实例响应
     */
    ProcessInstanceResponse startProcessInstance(StartProcessRequest request);

    /**
     * 根据流程实例 ID 获取流程实例信息
     * @param processInstanceId 流程实例ID
     * @return 流程实例响应
     */
    ProcessInstanceResponse getProcessInstanceById(String processInstanceId);

    /**
     * 根据流程定义 Key 获取所有活跃的流程实例
     * @param processDefinitionKey 流程定义 Key
     * @param tenantId 租户ID (可选)
     * @return 流程实例列表
     */
    List<ProcessInstanceResponse> getActiveProcessInstances(String processDefinitionKey, String tenantId);

    /**
     * 删除流程实例
     * @param processInstanceId 流程实例ID
     * @param deleteReason 删除原因
     */
    void deleteProcessInstance(String processInstanceId, String deleteReason);

    /**
     * 设置流程变量
     * @param processInstanceId 流程实例ID
     * @param variables 流程变量
     */
    void setProcessVariables(String processInstanceId, Map<String, Object> variables);

    /**
     * 获取流程变量
     * @param processInstanceId 流程实例ID
     * @return 流程变量
     */
    Map<String, Object> getProcessVariables(String processInstanceId);

    /**
     * 查询流程定义
     * @param key 流程定义 Key (可选)
     * @param tenantId 租户ID (可选)
     * @param latestVersion 是否只查询最新版本
     * @return 流程定义列表
     */
    List<ProcessDefinitionResponse> getProcessDefinitions(String key, String tenantId, boolean latestVersion);

    /**
     * 迁移流程实例
     * @param request 流程实例迁移请求
     */
    void migrateProcessInstances(ProcessInstanceMigrationRequest request);
}
