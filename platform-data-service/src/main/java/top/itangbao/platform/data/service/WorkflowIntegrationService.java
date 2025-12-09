package top.itangbao.platform.data.service;

import java.util.Map;

public interface WorkflowIntegrationService {

    String startProcessForData(String schemaName, Long dataId, String processKey);

    void completeTaskAndUpdateData(String taskId, Map<String, Object> variables);

}
