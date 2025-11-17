package top.itangbao.platform.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.itangbao.platform.metadata.domain.FieldType;

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
    private Long schemaId; // 所属模式ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
