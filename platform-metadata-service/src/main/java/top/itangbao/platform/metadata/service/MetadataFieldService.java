package top.itangbao.platform.metadata.service;

import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataFieldUpdateRequest;

import java.util.List;

public interface MetadataFieldService {
    MetadataFieldDTO createField(Long schemaId, MetadataFieldCreateRequest request);
    MetadataFieldDTO getFieldById(Long id);
    List<MetadataFieldDTO> getAllFieldsBySchemaId(Long schemaId);
    MetadataFieldDTO updateField(Long id, MetadataFieldUpdateRequest request);
    void deleteField(Long id);
    // 辅助方法，用于将实体转换为DTO
    MetadataFieldDTO convertToDTO(top.itangbao.platform.metadata.domain.MetadataField field);
}
