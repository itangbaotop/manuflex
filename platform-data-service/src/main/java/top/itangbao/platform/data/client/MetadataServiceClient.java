package top.itangbao.platform.data.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient; // 导入新的 Feign 客户端路径
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;

@Service
public class MetadataServiceClient {

    private final MetadataServiceFeignClient feignClient;

    @Autowired
    public MetadataServiceClient(MetadataServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public Mono<MetadataSchemaDTO> getSchemaByNameAndTenantId(String schemaName, String tenantId) {
        try {
            MetadataSchemaDTO schemaDTO = feignClient.getSchemaByNameAndTenantId(schemaName, tenantId);
            return Mono.just(schemaDTO);
        } catch (feign.FeignException.NotFound e) {
            return Mono.error(new ResourceNotFoundException("Metadata schema not found: " + e.getMessage(), e));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to retrieve metadata schema: " + e.getMessage(), e));
        }
    }

    public Mono<MetadataSchemaDTO> getSchemaById(Long schemaId) {
        try {
            MetadataSchemaDTO schemaDTO = feignClient.getSchemaById(schemaId);
            return Mono.just(schemaDTO);
        } catch (feign.FeignException.NotFound e) {
            return Mono.error(new ResourceNotFoundException("Metadata schema not found by ID: " + e.getMessage(), e));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to retrieve metadata schema by ID: " + e.getMessage(), e));
        }
    }
}
