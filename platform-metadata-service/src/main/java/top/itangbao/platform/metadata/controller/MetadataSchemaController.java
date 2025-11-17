package top.itangbao.platform.metadata.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 @PreAuthorize
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.metadata.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.dto.MetadataSchemaDTO;
import top.itangbao.platform.metadata.dto.MetadataSchemaUpdateRequest;
import top.itangbao.platform.metadata.service.MetadataSchemaService;

import java.util.List;

@RestController
@RequestMapping("/api/metadata/schemas") // 定义基础路径
public class MetadataSchemaController {

    private final MetadataSchemaService schemaService;

    @Autowired
    public MetadataSchemaController(MetadataSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * 创建元数据模式
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param request 创建请求体
     * @return 创建成功的模式信息
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<MetadataSchemaDTO> createSchema(@Valid @RequestBody MetadataSchemaCreateRequest request) {
        MetadataSchemaDTO createdSchema = schemaService.createSchema(request);
        return new ResponseEntity<>(createdSchema, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 获取元数据模式
     * 只有拥有 'ADMIN' 角色或属于该模式的租户的 'TENANT_ADMIN' 角色才能访问
     * (注意：这里简化为 ADMIN 或 TENANT_ADMIN 可以访问，实际可能需要更复杂的租户隔离逻辑)
     * @param id 模式ID
     * @return 模式信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // 暂时简化权限，实际需要根据 tenantId 动态判断
    public ResponseEntity<MetadataSchemaDTO> getSchemaById(@PathVariable Long id) {
        MetadataSchemaDTO schema = schemaService.getSchemaById(id);
        // 实际这里需要添加逻辑来验证当前用户是否属于该 schema.getTenantId()
        return ResponseEntity.ok(schema);
    }

    /**
     * 根据模式名称和租户ID获取元数据模式
     * 只有拥有 'ADMIN' 角色或属于该模式的租户的 'TENANT_ADMIN' 角色才能访问
     * @param name 模式名称
     * @param tenantId 租户ID
     * @return 模式信息
     */
    @GetMapping("/by-name")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // 暂时简化权限，实际需要根据 tenantId 动态判断
    public ResponseEntity<MetadataSchemaDTO> getSchemaByNameAndTenantId(
            @RequestParam String name,
            @RequestParam String tenantId) {
        MetadataSchemaDTO schema = schemaService.getSchemaByNameAndTenantId(name, tenantId);
        // 实际这里需要添加逻辑来验证当前用户是否属于该 tenantId
        return ResponseEntity.ok(schema);
    }

    /**
     * 根据租户ID获取所有元数据模式
     * 只有拥有 'ADMIN' 角色或属于该租户的 'TENANT_ADMIN' 角色才能访问
     * @param tenantId 租户ID
     * @return 模式列表
     */
    @GetMapping("/by-tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')") // 暂时简化权限，实际需要根据 tenantId 动态判断
    public ResponseEntity<List<MetadataSchemaDTO>> getAllSchemasByTenantId(@PathVariable String tenantId) {
        List<MetadataSchemaDTO> schemas = schemaService.getAllSchemasByTenantId(tenantId);
        // 实际这里需要添加逻辑来验证当前用户是否属于该 tenantId
        return ResponseEntity.ok(schemas);
    }

    /**
     * 更新元数据模式
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param id 模式ID
     * @param request 更新请求体
     * @return 更新后的模式信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<MetadataSchemaDTO> updateSchema(@PathVariable Long id, @Valid @RequestBody MetadataSchemaUpdateRequest request) {
        MetadataSchemaDTO updatedSchema = schemaService.updateSchema(id, request);
        return ResponseEntity.ok(updatedSchema);
    }

    /**
     * 删除元数据模式
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param id 模式ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteSchema(@PathVariable Long id) {
        schemaService.deleteSchema(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
