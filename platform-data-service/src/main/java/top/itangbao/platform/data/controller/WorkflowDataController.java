package top.itangbao.platform.data.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.data.service.WorkflowIntegrationService;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class WorkflowDataController {

    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;

    @PostMapping("/{schemaName}/{dataId}/start-workflow")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<Map<String, String>> startWorkflowForData(
            @PathVariable String schemaName,
            @PathVariable Long dataId,
            @RequestBody Map<String, String> request) {
        
        String processKey = request.get("processKey");
        String processInstanceId = workflowIntegrationService.startProcessForData(
            schemaName, dataId, processKey
        );
        
        return ResponseEntity.ok(Map.of("processInstanceId", processInstanceId));
    }
}
