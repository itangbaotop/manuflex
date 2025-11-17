package top.itangbao.platform.metadata.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.metadata.domain.MetadataField;
import top.itangbao.platform.metadata.domain.MetadataSchema;
import top.itangbao.platform.metadata.dto.*;
import top.itangbao.platform.metadata.exception.ResourceAlreadyExistsException;
import top.itangbao.platform.metadata.exception.ResourceNotFoundException;
import top.itangbao.platform.metadata.repository.MetadataFieldRepository;
import top.itangbao.platform.metadata.repository.MetadataSchemaRepository;
import top.itangbao.platform.metadata.service.MetadataSchemaService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MetadataSchemaServiceImpl implements MetadataSchemaService {

    private final MetadataSchemaRepository schemaRepository;
    private final MetadataFieldRepository fieldRepository; // 用于保存级联字段

    @Autowired
    public MetadataSchemaServiceImpl(MetadataSchemaRepository schemaRepository,
                                     MetadataFieldRepository fieldRepository) {
        this.schemaRepository = schemaRepository;
        this.fieldRepository = fieldRepository;
    }

    @Override
    @Transactional
    public MetadataSchemaDTO createSchema(MetadataSchemaCreateRequest request) {
        if (schemaRepository.existsByNameAndTenantId(request.getName(), request.getTenantId())) {
            throw new ResourceAlreadyExistsException("Metadata schema with name '" + request.getName() + "' already exists for tenant '" + request.getTenantId() + "'");
        }

        MetadataSchema schema = new MetadataSchema();
        schema.setName(request.getName());
        schema.setDescription(request.getDescription());
        schema.setTenantId(request.getTenantId());

        // 处理字段列表
        if (request.getFields() != null && !request.getFields().isEmpty()) {
            List<MetadataField> fields = request.getFields().stream()
                    .map(fieldReq -> {
                        MetadataField field = new MetadataField();
                        BeanUtils.copyProperties(fieldReq, field);
                        field.setSchema(schema); // 设置字段所属的模式
                        return field;
                    })
                    .collect(Collectors.toList());
            schema.setFields(fields);
        }

        MetadataSchema savedSchema = schemaRepository.save(schema);
        return convertToDTO(savedSchema);
    }

    @Override
    public MetadataSchemaDTO getSchemaById(Long id) {
        MetadataSchema schema = schemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + id));
        return convertToDTO(schema);
    }

    @Override
    public MetadataSchemaDTO getSchemaByNameAndTenantId(String name, String tenantId) {
        MetadataSchema schema = schemaRepository.findByNameAndTenantId(name, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + name + "' for tenant '" + tenantId + "'"));
        return convertToDTO(schema);
    }

    @Override
    public List<MetadataSchemaDTO> getAllSchemasByTenantId(String tenantId) {
        return schemaRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MetadataSchemaDTO updateSchema(Long id, MetadataSchemaUpdateRequest request) {
        MetadataSchema schema = schemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + id));

        if (request.getName() != null && !request.getName().equals(schema.getName()) && schemaRepository.existsByNameAndTenantId(request.getName(), schema.getTenantId())) {
            throw new ResourceAlreadyExistsException("Metadata schema with name '" + request.getName() + "' already exists for tenant '" + schema.getTenantId() + "'");
        }

        if (request.getName() != null) {
            schema.setName(request.getName());
        }
        if (request.getDescription() != null) {
            schema.setDescription(request.getDescription());
        }
        if (request.getTenantId() != null) {
            schema.setTenantId(request.getTenantId());
        }

        MetadataSchema updatedSchema = schemaRepository.save(schema);
        return convertToDTO(updatedSchema);
    }

    @Override
    @Transactional
    public void deleteSchema(Long id) {
        if (!schemaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Metadata schema not found with ID: " + id);
        }
        schemaRepository.deleteById(id);
    }

    @Override
    public MetadataSchemaDTO convertToDTO(MetadataSchema schema) {
        List<MetadataFieldDTO> fieldDTOs = schema.getFields().stream()
                .map(this::convertFieldToDTO)
                .collect(Collectors.toList());

        return MetadataSchemaDTO.builder()
                .id(schema.getId())
                .name(schema.getName())
                .description(schema.getDescription())
                .tenantId(schema.getTenantId())
                .createdAt(schema.getCreatedAt())
                .updatedAt(schema.getUpdatedAt())
                .fields(fieldDTOs)
                .build();
    }

    // 辅助方法：将 MetadataField 实体转换为 MetadataFieldDTO
    private MetadataFieldDTO convertFieldToDTO(MetadataField field) {
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
