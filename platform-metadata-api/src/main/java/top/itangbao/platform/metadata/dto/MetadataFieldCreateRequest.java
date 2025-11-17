package top.itangbao.platform.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.itangbao.platform.metadata.enums.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataFieldCreateRequest {
    @NotBlank(message = "Field name cannot be empty")
    @Size(min = 3, max = 100, message = "Field name must be between 3 and 100 characters")
    private String fieldName;

    @NotNull(message = "Field type cannot be null")
    private FieldType fieldType;

    private Boolean required = false;

    private String defaultValue;

    @Size(max = 500, message = "Validation rule cannot exceed 500 characters")
    private String validationRule;

    @Size(max = 1000, message = "Options cannot exceed 1000 characters")
    private String options; // JSON 字符串，例如 ["Option1", "Option2"]

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
