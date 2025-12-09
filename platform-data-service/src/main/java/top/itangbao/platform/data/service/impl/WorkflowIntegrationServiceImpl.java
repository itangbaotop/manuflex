package top.itangbao.platform.data.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.common.util.SecurityUtils;
import top.itangbao.platform.data.api.dto.DynamicDataResponse;
import top.itangbao.platform.data.service.DynamicDataService;
import top.itangbao.platform.data.service.WorkflowIntegrationService;
import top.itangbao.platform.workflow.api.client.WorkflowServiceFeignClient;
import top.itangbao.platform.workflow.api.dto.CompleteTaskRequest;
import top.itangbao.platform.workflow.api.dto.StartProcessRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowIntegrationServiceImpl implements WorkflowIntegrationService {

    @Autowired
    private DynamicDataService dynamicDataService;

    @Autowired
    private WorkflowServiceFeignClient workflowServiceFeignClient;

    /**
     * 启动流程并关联业务数据
     */
    @Transactional
    @Override
    public String startProcessForData(String schemaName, Long dataId, String processKey) {
        CustomUserDetails loginUser = SecurityUtils.getLoginUser();

        String tenantId = loginUser.getTenantId();
        // 1. 获取业务数据
        DynamicDataResponse response = dynamicDataService.getDynamicDataById(tenantId, schemaName, dataId);
        Map<String, Object> data = response.getData();

        // 2. 准备流程变量
        Map<String, Object> variables = new HashMap<>(data);
        variables.put("schemaName", schemaName);
        variables.put("dataId", dataId);
        variables.put("initiator", loginUser.getUsername());

        // 3. 启动流程
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey(processKey);
        request.setBusinessKey(schemaName + "_" + dataId);
        request.setVariables(variables);
        request.setTenantId(tenantId);

        String processInstanceId = workflowServiceFeignClient.startProcessInstance(request).getId();

        // 4. 更新业务数据状态
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("workflowStatus", "IN_PROGRESS");
        updateData.put("processInstanceId", processInstanceId);
        dynamicDataService.updateDynamicData(tenantId, schemaName, dataId, updateData);

        return processInstanceId;
    }

    /**
     * 完成任务并更新业务数据
     */
    @Transactional
    @Override
    public void completeTaskAndUpdateData(String taskId, Map<String, Object> variables) {
        // 从变量中获取业务数据信息
        String schemaName = (String) variables.get("schemaName");
        Long dataId = Long.valueOf(variables.get("dataId").toString());
        String tenantId = (String) variables.get("tenantId");

        // 完成任务
        CompleteTaskRequest request = new CompleteTaskRequest();
        request.setTaskId(taskId);
        request.setVariables(variables);

        workflowServiceFeignClient.completeTask(request);

        // 更新业务数据（如果有审批意见等）
        if (variables.containsKey("approved")) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("workflowStatus",
                    Boolean.TRUE.equals(variables.get("approved")) ? "APPROVED" : "REJECTED");
            updateData.put("approvalComment", variables.get("comment"));
            dynamicDataService.updateDynamicData(tenantId, schemaName, dataId, updateData);
        }
    }
}
