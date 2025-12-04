package top.itangbao.platform.metadata.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import top.itangbao.platform.common.enums.FieldType;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mf_metadata_fields") // 映射到 mf_metadata_fields 表
public class MetadataField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fieldName; // 字段名称，例如: sampleName, testDate, batchNumber

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING) // 枚举类型存储为字符串
    private FieldType fieldType; // 字段类型，例如: STRING, NUMBER, DATE, BOOLEAN, ENUM

    @Column(nullable = false)
    private Boolean required = false; // 是否必填

    @Column(length = 255)
    private String defaultValue; // 默认值

    @Column(length = 500)
    private String validationRule; // 校验规则 (可以是正则表达式、范围等)

    @Column(length = 1000)
    private String options; // 如果 fieldType 是 ENUM，这里存储选项的 JSON 字符串

    @Column(length = 255)
    private String description; // 字段描述

    @Column(name = "related_schema_name", length = 50)
    private String relatedSchemaName; // 关联的目标模型 (e.g. "Car")

    @Column(name = "related_field_name", length = 50)
    private String relatedFieldName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 多个字段属于一个模式 (多对一关系)
    @ManyToOne(fetch = FetchType.LAZY) // 延迟加载，避免循环引用和不必要的加载
    @JoinColumn(name = "schema_id", nullable = false) // 外键列
    private MetadataSchema schema;



    public static MetadataFieldDTO getMetadataFieldDTO(MetadataField field) {
        return MetadataFieldDTO.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .required(field.getRequired())
                .defaultValue(field.getDefaultValue())
                .validationRule(field.getValidationRule())
                .options(field.getOptions())
                .description(field.getDescription())
                .relatedSchemaName(field.getRelatedSchemaName())
                .relatedFieldName(field.getRelatedFieldName())
                .schemaId(field.getSchema() != null ? field.getSchema().getId() : null)
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }
}
