package top.itangbao.platform.data.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 @PreAuthorize
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.data.dto.DynamicDataRequest;
import top.itangbao.platform.data.dto.DynamicDataResponse;
import top.itangbao.platform.data.service.DynamicDataService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data/{tenantId}/{schemaName}") // 定义基础路径，包含租户ID和模式名称
public class DynamicDataController {

    private final DynamicDataService dynamicDataService;

    @Autowired
    public DynamicDataController(DynamicDataService dynamicDataService) {
        this.dynamicDataService = dynamicDataService;
    }

    /**
     * 根据元数据模式创建或更新动态数据表
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 元数据模式ID
     * @return 成功响应
     */
    @PostMapping("/table/{schemaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> createOrUpdateDynamicTable(@PathVariable Long schemaId) {
        dynamicDataService.createOrUpdateDynamicTable(schemaId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 根据元数据模式删除动态数据表
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 元数据模式ID
     * @return 无内容响应
     */
    @DeleteMapping("/table/{schemaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDynamicTable(@PathVariable Long schemaId) {
        dynamicDataService.deleteDynamicTable(schemaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 插入动态数据
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param request 动态数据请求体 (包含实际数据)
     * @return 插入后的数据响应
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'data:create'
    public ResponseEntity<DynamicDataResponse> insertDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @Valid @RequestBody DynamicDataRequest request) {
        // 确保请求中的 tenantId 和 schemaName 与路径变量一致
        if (!tenantId.equals(request.getTenantId()) || !schemaName.equals(request.getSchemaName())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DynamicDataResponse response = dynamicDataService.insertDynamicData(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 查询动态数据
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     * @return 动态数据响应
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'data:read'
    public ResponseEntity<DynamicDataResponse> getDynamicDataById(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id) {
        DynamicDataResponse response = dynamicDataService.getDynamicDataById(tenantId, schemaName, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询所有动态数据
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @return 动态数据列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'data:read_all'
    public ResponseEntity<List<DynamicDataResponse>> getAllDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName) {
        List<DynamicDataResponse> responses = dynamicDataService.getAllDynamicData(tenantId, schemaName);
        return ResponseEntity.ok(responses);
    }

    /**
     * 更新动态数据
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     * @param updates 要更新的字段和值
     * @return 更新后的数据响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'data:update'
    public ResponseEntity<DynamicDataResponse> updateDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) { // 这里不使用 @Valid，因为 Map 中的键值是动态的
        DynamicDataResponse response = dynamicDataService.updateDynamicData(tenantId, schemaName, id, updates);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除动态数据
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // TODO: 细化为更具体的权限，例如 'data:delete'
    public ResponseEntity<Void> deleteDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id) {
        dynamicDataService.deleteDynamicData(tenantId, schemaName, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
