package top.itangbao.platform.lims.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.lims.dto.TestItemRequest;
import top.itangbao.platform.lims.dto.TestItemResponse;
import top.itangbao.platform.lims.service.TestItemService;

import java.util.List;

@RestController
@RequestMapping("/api/lims/{tenantId}/test-items")
public class TestItemController {

    private final TestItemService testItemService;

    @Autowired
    public TestItemController(TestItemService testItemService) {
        this.testItemService = testItemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('lims:testitem:create', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<TestItemResponse> createTestItem(
            @PathVariable String tenantId,
            @Valid @RequestBody TestItemRequest request) {
        TestItemResponse response = testItemService.createTestItem(tenantId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{testItemId}")
    @PreAuthorize("hasAnyAuthority('lims:testitem:read', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<TestItemResponse> getTestItemById(
            @PathVariable String tenantId,
            @PathVariable Long testItemId) {
        TestItemResponse response = testItemService.getTestItemById(tenantId, testItemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('lims:testitem:read_all', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<TestItemResponse>> getAllTestItems(
            @PathVariable String tenantId) {
        List<TestItemResponse> responses = testItemService.getAllTestItems(tenantId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{testItemId}")
    @PreAuthorize("hasAnyAuthority('lims:testitem:update', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<TestItemResponse> updateTestItem(
            @PathVariable String tenantId,
            @PathVariable Long testItemId,
            @Valid @RequestBody TestItemRequest request) {
        TestItemResponse response = testItemService.updateTestItem(tenantId, testItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{testItemId}")
    @PreAuthorize("hasAnyAuthority('lims:testitem:delete', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> deleteTestItem(
            @PathVariable String tenantId,
            @PathVariable Long testItemId) {
        testItemService.deleteTestItem(tenantId, testItemId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
