package top.itangbao.platform.lims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleRequest {
    @NotBlank(message = "Sample name cannot be empty")
    private String sampleName;

    @NotBlank(message = "Batch number cannot be empty")
    private String batchNumber;

    @NotNull(message = "Collection date cannot be null")
    private String collectionDate; // 使用 String 方便前端传递，后端解析

    private String status; // 例如: "RECEIVED", "IN_TEST", "COMPLETED"

    private Map<String, Object> customFields; // 用于存储动态字段数据
}
