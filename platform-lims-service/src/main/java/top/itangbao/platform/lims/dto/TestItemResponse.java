package top.itangbao.platform.lims.dto;

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
public class TestItemResponse {
    private Long id;
    private String itemName;
    private String methodReference;
    private Double price;
    private String description;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> customFields;
}
