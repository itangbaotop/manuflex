package top.itangbao.platform.metadata.dto;

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
public class MetadataFieldUpdateRequest {
    @Size(min = 3, max = 100, message = "Field name must be between 3 and 100 characters")
    private String fieldName;

    private FieldType fieldType;

    private Boolean required;

    private String defaultValue;

    @Size(max = 500, message = "Validation rule cannot exceed 500 characters")
    private String validationRule;

    @Size(max = 1000, message = "Options cannot exceed 1000 characters")
    private String options;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
