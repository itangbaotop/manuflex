package top.itangbao.platform.data.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.data.api.dto.*;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端，用于调用 Data Service
 */
@FeignClient(name = "data-service", url = "${data.service.url}")
public interface DataServiceFeignClient {

    // --- 动态表管理 API ---
    @PostMapping("/api/data/{tenantId}/{schemaName}/table/{schemaId}")
    void createOrUpdateDynamicTable(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @PathVariable("schemaId") Long schemaId);

    @DeleteMapping("/api/data/{tenantId}/{schemaName}/table/{schemaId}")
    void deleteDynamicTable(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @PathVariable("schemaId") Long schemaId);

    // --- 动态数据 CRUD API ---
    @PostMapping("/api/data/{tenantId}/{schemaName}")
    DynamicDataResponse insertDynamicData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            DynamicDataRequest request); // 请求体在 Feign 中直接传递

    @GetMapping("/api/data/{tenantId}/{schemaName}/{id}")
    DynamicDataResponse getDynamicDataById(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @PathVariable("id") Long id);

    @GetMapping("/api/data/{tenantId}/{schemaName}")
    PageResponseDTO<DynamicDataResponse> getAllDynamicData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @SpringQueryMap PageRequestDTO pageRequest,
            @SpringQueryMap FilterRequestDTO filterRequest);

    @PutMapping("/api/data/{tenantId}/{schemaName}/{id}")
    DynamicDataResponse updateDynamicData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @PathVariable("id") Long id,
            Map<String, Object> updates); // 请求体在 Feign 中直接传递 Map

    @DeleteMapping("/api/data/{tenantId}/{schemaName}/{id}")
    void deleteDynamicData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @PathVariable("id") Long id);

    @PostMapping(value = "/api/data/{tenantId}/{schemaName}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DataImportResponse importData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @RequestPart("file") MultipartFile file);

    @GetMapping(value = "/api/data/{tenantId}/{schemaName}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] exportData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName,
            @SpringQueryMap FilterRequestDTO filterRequest, // 导出也支持过滤
            @RequestParam(value = "format", defaultValue = "csv") String format); // 导出格式 (csv, excel)

}
