package top.itangbao.platform.metadata.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataSchemaCreateRequest {
    @NotBlank(message = "Schema name cannot be empty")
    @Size(min = 3, max = 100, message = "Schema name must be between 3 and 100 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotBlank(message = "Tenant ID cannot be empty")
    @Size(max = 50, message = "Tenant ID cannot exceed 50 characters")
    private String tenantId;

    @Valid // 嵌套校验字段列表
    private List<MetadataFieldCreateRequest> fields;


    private Boolean workflowEnabled = false;

    private String workflowProcessKey;

    private String workflowFormKey;
}
