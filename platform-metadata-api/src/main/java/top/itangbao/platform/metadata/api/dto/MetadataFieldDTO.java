package top.itangbao.platform.metadata.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.itangbao.platform.common.enums.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataFieldDTO {
    private Long id;
    private String fieldName;
    private FieldType fieldType;
    private Boolean required;
    private String defaultValue;
    private String validationRule;
    private String options;
    private String description;
    private String relatedSchemaName;
    private String relatedFieldName;
    private Long schemaId; // 所属模式ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
