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
public class TestResultResponse {
    private Long id;
    private Long sampleId;
    private Long testItemId;
    private String resultValue;
    private String unit;
    private String status;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> customFields;
}
