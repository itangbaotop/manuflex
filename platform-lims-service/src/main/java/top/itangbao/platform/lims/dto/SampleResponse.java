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
public class SampleResponse {
    private Long id;
    private String sampleName;
    private String batchNumber;
    private String collectionDate;
    private String status;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> customFields;
}
