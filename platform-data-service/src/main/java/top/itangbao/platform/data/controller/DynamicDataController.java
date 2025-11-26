package top.itangbao.platform.data.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 @PreAuthorize
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.data.service.DynamicDataService;

import java.io.IOException;
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
    @PreAuthorize("hasAnyAuthority('data:create', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('data:read', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyAuthority('data:read_all', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<PageResponseDTO<DynamicDataResponse>> getAllDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> filters) {

        // 构建 PageRequestDTO
        PageRequestDTO pageRequest = PageRequestDTO.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        // 构建 FilterRequestDTO (移除分页和排序相关的参数)
        FilterRequestDTO filterRequest = FilterRequestDTO.builder()
                .filters(new java.util.HashMap<>(filters))
                .build();

        // 移除 filters 中与分页和排序相关的参数，避免传递给 SQL 过滤
        filterRequest.getFilters().remove("page");
        filterRequest.getFilters().remove("size");
        filterRequest.getFilters().remove("sortBy");
        filterRequest.getFilters().remove("sortOrder");


        PageResponseDTO<DynamicDataResponse> responses = dynamicDataService.getAllDynamicData(tenantId, schemaName, pageRequest, filterRequest);
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
    @PreAuthorize("hasAnyAuthority('data:update', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('data:delete', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id) {
        dynamicDataService.deleteDynamicData(tenantId, schemaName, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 导入数据 (支持 CSV)
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param file 导入文件
     * @return 导入结果
     * @throws IOException
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('data:import', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<DataImportResponse> importData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DataImportResponse response = dynamicDataService.importData(tenantId, schemaName, file);
        return ResponseEntity.ok(response);
    }

    /**
     * 导出数据 (支持 CSV)
     * 只有拥有 'ADMIN' 或 'TENANT_ADMIN' 角色或特定权限的用户才能访问
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param filters 过滤条件
     * @param format 导出格式 (csv, excel)
     * @return 导出文件的字节数组
     * @throws IOException
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('data:export', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<byte[]> exportData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "csv") String format) throws IOException {

        FilterRequestDTO filterRequest = FilterRequestDTO.builder()
                .filters(new java.util.HashMap<>(filters))
                .build();

        // 移除 filters 中与分页和排序相关的参数
        filterRequest.getFilters().remove("page");
        filterRequest.getFilters().remove("size");
        filterRequest.getFilters().remove("sortBy");
        filterRequest.getFilters().remove("sortOrder");

        byte[] fileBytes = dynamicDataService.exportData(tenantId, schemaName, filterRequest, format);

        HttpHeaders headers = new HttpHeaders();
        String fileName = schemaName + "_data." + format;
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}
