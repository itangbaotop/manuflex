package top.itangbao.platform.metadata.service;

import top.itangbao.platform.metadata.api.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaUpdateRequest;

import java.util.List;

public interface MetadataSchemaService {
    MetadataSchemaDTO createSchema(MetadataSchemaCreateRequest request);
    MetadataSchemaDTO getSchemaById(Long id);
    MetadataSchemaDTO getSchemaByNameAndTenantId(String name, String tenantId);
    List<MetadataSchemaDTO> getAllSchemasByTenantId(String tenantId);
    MetadataSchemaDTO updateSchema(Long id, MetadataSchemaUpdateRequest request);
    void deleteSchema(Long id);
    // 辅助方法，用于将实体转换为DTO
    MetadataSchemaDTO convertToDTO(top.itangbao.platform.metadata.domain.MetadataSchema schema);
}
