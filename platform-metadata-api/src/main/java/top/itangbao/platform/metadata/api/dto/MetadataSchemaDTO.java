package top.itangbao.platform.metadata.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataSchemaDTO {
    private Long id;
    private String name;
    private String description;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MetadataFieldDTO> fields; // 包含字段列表
}
