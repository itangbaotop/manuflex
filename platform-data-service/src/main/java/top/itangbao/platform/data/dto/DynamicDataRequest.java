package top.itangbao.platform.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicDataRequest {
    @NotBlank(message = "Tenant ID cannot be empty")
    private String tenantId;

    @NotBlank(message = "Schema name cannot be empty")
    private String schemaName;

    private Map<String, Object> data; // 动态数据，键是字段名，值是字段值
}
