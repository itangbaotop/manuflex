package top.itangbao.platform.metadata.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceAlreadyExistsException;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.metadata.domain.MetadataField;
import top.itangbao.platform.metadata.domain.MetadataSchema;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataFieldUpdateRequest;
import top.itangbao.platform.metadata.repository.MetadataFieldRepository;
import top.itangbao.platform.metadata.repository.MetadataSchemaRepository;
import top.itangbao.platform.metadata.service.MetadataFieldService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MetadataFieldServiceImpl implements MetadataFieldService {

    private final MetadataFieldRepository fieldRepository;
    private final MetadataSchemaRepository schemaRepository;

    @Autowired
    public MetadataFieldServiceImpl(MetadataFieldRepository fieldRepository, MetadataSchemaRepository schemaRepository) {
        this.fieldRepository = fieldRepository;
        this.schemaRepository = schemaRepository;
    }

    @Override
    @Transactional
    public MetadataFieldDTO createField(Long schemaId, MetadataFieldCreateRequest request) {
        MetadataSchema schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + schemaId));

        if (fieldRepository.existsByFieldNameAndSchema(request.getFieldName(), schema)) {
            throw new ResourceAlreadyExistsException("Metadata field with name '" + request.getFieldName() + "' already exists in schema '" + schema.getName() + "'");
        }

        MetadataField field = new MetadataField();
        BeanUtils.copyProperties(request, field);
        field.setSchema(schema); // 设置所属模式

        MetadataField savedField = fieldRepository.save(field);
        return convertToDTO(savedField);
    }

    @Override
    public MetadataFieldDTO getFieldById(Long id) {
        MetadataField field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata field not found with ID: " + id));
        return convertToDTO(field);
    }

    @Override
    public List<MetadataFieldDTO> getAllFieldsBySchemaId(Long schemaId) {
        MetadataSchema schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + schemaId));
        return fieldRepository.findBySchema(schema).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MetadataFieldDTO updateField(Long id, MetadataFieldUpdateRequest request) {
        MetadataField field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata field not found with ID: " + id));

        // 检查更新后的字段名是否已存在于同一模式下 (排除当前字段自身)
        if (request.getFieldName() != null && !request.getFieldName().equals(field.getFieldName()) && fieldRepository.existsByFieldNameAndSchema(request.getFieldName(), field.getSchema())) {
            throw new ResourceAlreadyExistsException("Metadata field with name '" + request.getFieldName() + "' already exists in schema '" + field.getSchema().getName() + "'");
        }

        if (request.getFieldName() != null) {
            field.setFieldName(request.getFieldName());
        }
        if (request.getFieldType() != null) {
            field.setFieldType(request.getFieldType());
        }
        if (request.getRequired() != null) {
            field.setRequired(request.getRequired());
        }
        if (request.getDefaultValue() != null) {
            field.setDefaultValue(request.getDefaultValue());
        }
        if (request.getValidationRule() != null) {
            field.setValidationRule(request.getValidationRule());
        }
        if (request.getOptions() != null) {
            field.setOptions(request.getOptions());
        }
        if (request.getDescription() != null) {
            field.setDescription(request.getDescription());
        }

        MetadataField updatedField = fieldRepository.save(field);
        return convertToDTO(updatedField);
    }

    @Override
    @Transactional
    public void deleteField(Long id) {
        if (!fieldRepository.existsById(id)) {
            throw new ResourceNotFoundException("Metadata field not found with ID: " + id);
        }
        fieldRepository.deleteById(id);
    }

    @Override
    public MetadataFieldDTO convertToDTO(MetadataField field) {
        return MetadataFieldDTO.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .required(field.getRequired())
                .defaultValue(field.getDefaultValue())
                .validationRule(field.getValidationRule())
                .options(field.getOptions())
                .description(field.getDescription())
                .schemaId(field.getSchema() != null ? field.getSchema().getId() : null)
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }
}
