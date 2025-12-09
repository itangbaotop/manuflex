package top.itangbao.platform.metadata.api.dto;

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
public class MetadataSchemaUpdateRequest {
    @Size(min = 3, max = 100, message = "Schema name must be between 3 and 100 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 50, message = "Tenant ID cannot exceed 50 characters")
    private String tenantId;

    private Boolean workflowEnabled = false;

    private String workflowProcessKey;

    private String workflowFormKey;
}
