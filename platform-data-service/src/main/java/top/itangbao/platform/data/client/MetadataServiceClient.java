package top.itangbao.platform.data.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import top.itangbao.platform.metadata.dto.MetadataSchemaDTO; // 引入 MetadataService 的 DTO

import java.util.List;

@Service
public class MetadataServiceClient {

    private final WebClient metadataServiceWebClient;

    @Autowired
    public MetadataServiceClient(WebClient metadataServiceWebClient) {
        this.metadataServiceWebClient = metadataServiceWebClient;
    }

    /**
     * 根据模式名称和租户ID获取元数据模式
     * @param schemaName 模式名称
     * @param tenantId 租户ID
     * @return MetadataSchemaDTO
     */
    public Mono<MetadataSchemaDTO> getSchemaByNameAndTenantId(String schemaName, String tenantId) {
        return metadataServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/metadata/schemas/by-name")
                        .queryParam("name", schemaName)
                        .queryParam("tenantId", tenantId)
                        .build())
                // 在实际生产中，这里需要添加请求头，用于微服务间的认证
                // .header("X-Internal-Token", "your-internal-token")
                .retrieve()
                .bodyToMono(MetadataSchemaDTO.class)
                .onErrorResume(e -> {
                    // 错误处理，例如当模式不存在时
                    return Mono.error(new RuntimeException("Failed to retrieve metadata schema: " + e.getMessage(), e));
                });
    }

    /**
     * 根据模式ID获取元数据模式
     * @param schemaId 模式ID
     * @return MetadataSchemaDTO
     */
    public Mono<MetadataSchemaDTO> getSchemaById(Long schemaId) {
        return metadataServiceWebClient.get()
                .uri("/api/metadata/schemas/{id}", schemaId)
                .retrieve()
                .bodyToMono(MetadataSchemaDTO.class)
                .onErrorResume(e -> {
                    return Mono.error(new RuntimeException("Failed to retrieve metadata schema by ID: " + e.getMessage(), e));
                });
    }

    // 如果需要，可以添加获取所有模式、所有字段等方法
}
