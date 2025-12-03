package top.itangbao.platform.metadata.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 @PreAuthorize
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataFieldUpdateRequest;
import top.itangbao.platform.metadata.service.MetadataFieldService;

import java.util.List;

@RestController
@RequestMapping("/api/metadata/schemas/{schemaId}/fields") // 定义基础路径，嵌套在 schema 下
public class MetadataFieldController {

    private final MetadataFieldService fieldService;

    @Autowired
    public MetadataFieldController(MetadataFieldService fieldService) {
        this.fieldService = fieldService;
    }

    /**
     * 为指定模式创建元数据字段
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 所属模式ID
     * @param request 创建请求体
     * @return 创建成功的字段信息
     */
    @PostMapping
    @PreAuthorize("hasAuthority('schema:write')")
    public ResponseEntity<MetadataFieldDTO> createField(
            @PathVariable Long schemaId,
            @Valid @RequestBody MetadataFieldCreateRequest request) {
        MetadataFieldDTO createdField = fieldService.createField(schemaId, request);
        return new ResponseEntity<>(createdField, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 获取元数据字段
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 所属模式ID (仅用于路径，实际不一定需要)
     * @param id 字段ID
     * @return 字段信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('schema:read')")
    public ResponseEntity<MetadataFieldDTO> getFieldById(
            @PathVariable Long schemaId, // 路径变量，但在这个方法中可能不直接使用
            @PathVariable Long id) {
        MetadataFieldDTO field = fieldService.getFieldById(id);
        return ResponseEntity.ok(field);
    }

    /**
     * 获取指定模式下的所有元数据字段
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 所属模式ID
     * @return 字段列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('schema:read')")
    public ResponseEntity<List<MetadataFieldDTO>> getAllFieldsBySchemaId(@PathVariable Long schemaId) {
        List<MetadataFieldDTO> fields = fieldService.getAllFieldsBySchemaId(schemaId);
        return ResponseEntity.ok(fields);
    }

    /**
     * 更新元数据字段
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 所属模式ID (仅用于路径，实际不一定需要)
     * @param id 字段ID
     * @param request 更新请求体
     * @return 更新后的字段信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('schema:write')")
    public ResponseEntity<MetadataFieldDTO> updateField(
            @PathVariable Long schemaId, // 路径变量，但在这个方法中可能不直接使用
            @PathVariable Long id,
            @Valid @RequestBody MetadataFieldUpdateRequest request) {
        MetadataFieldDTO updatedField = fieldService.updateField(id, request);
        return ResponseEntity.ok(updatedField);
    }

    /**
     * 删除元数据字段
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param schemaId 所属模式ID (仅用于路径，实际不一定需要)
     * @param id 字段ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('schema:write')")
    public ResponseEntity<Void> deleteField(
            @PathVariable Long schemaId, // 路径变量，但在这个方法中可能不直接使用
            @PathVariable Long id) {
        fieldService.deleteField(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
