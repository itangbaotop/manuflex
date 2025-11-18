package top.itangbao.platform.metadata.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;

/**
 * Feign 客户端，用于调用 Metadata Service
 * name: 服务的逻辑名称 (未来可以与服务注册中心集成)
 * url: Metadata Service 的实际地址 (本地开发时直接指定，生产环境通过服务注册中心获取)
 */
@FeignClient(name = "metadata-service", url = "${metadata.service.url}") // 使用 application.yml 中的配置
public interface MetadataServiceFeignClient {

    @GetMapping("/api/metadata/schemas/by-name")
    MetadataSchemaDTO getSchemaByNameAndTenantId(@RequestParam("name") String name, @RequestParam("tenantId") String tenantId);

    @GetMapping("/api/metadata/schemas/{id}")
    MetadataSchemaDTO getSchemaById(@PathVariable("id") Long id);

    // 可以添加更多 Metadata Service 的 API 方法
}
