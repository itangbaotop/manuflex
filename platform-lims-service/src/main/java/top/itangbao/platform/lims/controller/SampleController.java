package top.itangbao.platform.lims.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.lims.dto.SampleRequest;
import top.itangbao.platform.lims.dto.SampleResponse;
import top.itangbao.platform.lims.service.SampleService;

import java.util.List;

@RestController
@RequestMapping("/api/lims/{tenantId}/samples") // 定义基础路径，包含租户ID
public class SampleController {

    private final SampleService sampleService;

    @Autowired
    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    /**
     * 创建样品
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param request 样品请求
     * @return 创建的样品响应
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'lims:sample:create'
    public ResponseEntity<SampleResponse> createSample(
            @PathVariable String tenantId,
            @Valid @RequestBody SampleRequest request) {
        // 确保请求中的 tenantId 与路径变量一致 (可选，但推荐)
        // if (!tenantId.equals(request.getTenantId())) { return new ResponseEntity<>(HttpStatus.BAD_REQUEST); }
        SampleResponse response = sampleService.createSample(tenantId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 获取样品
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     * @return 样品响应
     */
    @GetMapping("/{sampleId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'lims:sample:read'
    public ResponseEntity<SampleResponse> getSampleById(
            @PathVariable String tenantId,
            @PathVariable Long sampleId) {
        SampleResponse response = sampleService.getSampleById(tenantId, sampleId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有样品
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @return 样品列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'lims:sample:read_all'
    public ResponseEntity<List<SampleResponse>> getAllSamples(
            @PathVariable String tenantId) {
        List<SampleResponse> responses = sampleService.getAllSamples(tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 更新样品
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     * @param request 样品请求
     * @return 更新后的样品响应
     */
    @PutMapping("/{sampleId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'lims:sample:update'
    public ResponseEntity<SampleResponse> updateSample(
            @PathVariable String tenantId,
            @PathVariable Long sampleId,
            @Valid @RequestBody SampleRequest request) {
        SampleResponse response = sampleService.updateSample(tenantId, sampleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除样品
     * 只有拥有 'ROLE_ADMIN' 或 'ROLE_TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     * @return 无内容响应
     */
    @DeleteMapping("/{sampleId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'lims:sample:delete'
    public ResponseEntity<Void> deleteSample(
            @PathVariable String tenantId,
            @PathVariable Long sampleId) {
        sampleService.deleteSample(tenantId, sampleId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
