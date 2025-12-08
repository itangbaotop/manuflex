package top.itangbao.platform.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowIntegrationService {

    @Autowired
    private DynamicDataService dynamicDataService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String WORKFLOW_SERVICE_URL = "http://localhost:8085/api/workflow";

    /**
     * 启动流程并关联业务数据
     */
    @Transactional
    public String startProcessForData(String schemaName, Long dataId, String processKey, String tenantId) {
        // 1. 获取业务数据
        Map<String, Object> data = dynamicDataService.getDataById(schemaName, dataId, tenantId);
        
        // 2. 准备流程变量
        Map<String, Object> variables = new HashMap<>(data);
        variables.put("schemaName", schemaName);
        variables.put("dataId", dataId);
        variables.put("initiator", "admin"); // TODO: 从当前用户获取
        
        // 3. 启动流程
        Map<String, Object> request = new HashMap<>();
        request.put("processDefinitionKey", processKey);
        request.put("businessKey", schemaName + "_" + dataId);
        request.put("variables", variables);
        request.put("tenantId", tenantId);
        
        Map<String, Object> response = restTemplate.postForObject(
            WORKFLOW_SERVICE_URL + "/process-instances",
            request,
            Map.class
        );
        
        String processInstanceId = (String) response.get("id");
        
        // 4. 更新业务数据状态
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("workflowStatus", "IN_PROGRESS");
        updateData.put("processInstanceId", processInstanceId);
        dynamicDataService.updateData(schemaName, dataId, updateData, tenantId);
        
        return processInstanceId;
    }

    /**
     * 完成任务并更新业务数据
     */
    @Transactional
    public void completeTaskAndUpdateData(String taskId, Map<String, Object> variables) {
        // 从变量中获取业务数据信息
        String schemaName = (String) variables.get("schemaName");
        Long dataId = Long.valueOf(variables.get("dataId").toString());
        String tenantId = (String) variables.get("tenantId");
        
        // 完成任务
        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("variables", variables);
        
        restTemplate.postForObject(
            WORKFLOW_SERVICE_URL + "/tasks/complete",
            request,
            Void.class
        );
        
        // 更新业务数据（如果有审批意见等）
        if (variables.containsKey("approved")) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("workflowStatus", 
                Boolean.TRUE.equals(variables.get("approved")) ? "APPROVED" : "REJECTED");
            updateData.put("approvalComment", variables.get("comment"));
            dynamicDataService.updateData(schemaName, dataId, updateData, tenantId);
        }
    }
}
