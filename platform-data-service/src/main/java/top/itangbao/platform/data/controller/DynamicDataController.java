package top.itangbao.platform.data.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.data.service.DynamicDataService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DynamicDataController {

    private final DynamicDataService dynamicDataService;

    @Autowired
    public DynamicDataController(DynamicDataService dynamicDataService) {
        this.dynamicDataService = dynamicDataService;
    }

    // ==========================================
    // 1. 表结构管理 (DDL)
    // ==========================================

    /**
     * 同步数据库表 (Create/Update Table)
     * URL: POST /api/data/tables/{schemaId}
     */
    @PostMapping("/tables/{schemaId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> createOrUpdateDynamicTable(@PathVariable Long schemaId) {
        dynamicDataService.createOrUpdateDynamicTable(schemaId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 删除数据库表 (Drop Table)
     * URL: DELETE /api/data/tables/{schemaId}
     */
    @DeleteMapping("/tables/{schemaId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDynamicTable(@PathVariable Long schemaId) {
        dynamicDataService.deleteDynamicTable(schemaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==========================================
    // 2. 数据增删改查 (DML)
    // URL 必须包含 /{tenantId}/{schemaName}
    // ==========================================

    @PostMapping("/{tenantId}/{schemaName}")
    @PreAuthorize("hasAnyAuthority('data:create', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<DynamicDataResponse> insertDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @Valid @RequestBody DynamicDataRequest request) {
        if (!tenantId.equals(request.getTenantId()) || !schemaName.equals(request.getSchemaName())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DynamicDataResponse response = dynamicDataService.insertDynamicData(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{tenantId}/{schemaName}/{id}")
    @PreAuthorize("hasAnyAuthority('data:read', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<DynamicDataResponse> getDynamicDataById(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id) {
        DynamicDataResponse response = dynamicDataService.getDynamicDataById(tenantId, schemaName, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}/{schemaName}")
    @PreAuthorize("hasAnyAuthority('data:read_all', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<PageResponseDTO<DynamicDataResponse>> getAllDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> filters) {

        PageRequestDTO pageRequest = PageRequestDTO.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        FilterRequestDTO filterRequest = FilterRequestDTO.builder()
                .filters(new java.util.HashMap<>(filters))
                .build();

        filterRequest.getFilters().remove("page");
        filterRequest.getFilters().remove("size");
        filterRequest.getFilters().remove("sortBy");
        filterRequest.getFilters().remove("sortOrder");

        PageResponseDTO<DynamicDataResponse> responses = dynamicDataService.getAllDynamicData(tenantId, schemaName, pageRequest, filterRequest);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{tenantId}/{schemaName}/{id}")
    @PreAuthorize("hasAnyAuthority('data:update', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<DynamicDataResponse> updateDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        DynamicDataResponse response = dynamicDataService.updateDynamicData(tenantId, schemaName, id, updates);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}/{schemaName}/{id}")
    @PreAuthorize("hasAnyAuthority('data:delete', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDynamicData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @PathVariable Long id) {
        dynamicDataService.deleteDynamicData(tenantId, schemaName, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/{tenantId}/{schemaName}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    @GetMapping(value = "/{tenantId}/{schemaName}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('data:export', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<byte[]> exportData(
            @PathVariable String tenantId,
            @PathVariable String schemaName,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "csv") String format) throws IOException {

        FilterRequestDTO filterRequest = FilterRequestDTO.builder()
                .filters(new java.util.HashMap<>(filters))
                .build();

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