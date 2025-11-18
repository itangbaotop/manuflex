package top.itangbao.platform.data.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.data.api.dto.DynamicDataRequest;
import top.itangbao.platform.data.api.dto.DynamicDataResponse;

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
    List<DynamicDataResponse> getAllDynamicData(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("schemaName") String schemaName);

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
}
