package top.itangbao.platform.data.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicDataResponse {
    private Long id;
    private String tenantId;
    private String schemaName;
    private Map<String, Object> data; // 动态数据
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
