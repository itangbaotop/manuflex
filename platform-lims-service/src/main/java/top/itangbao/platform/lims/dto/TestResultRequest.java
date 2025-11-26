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
public class TestResultRequest {
    @NotNull(message = "Sample ID cannot be null")
    private Long sampleId;

    @NotNull(message = "Test item ID cannot be null")
    private Long testItemId;

    @NotBlank(message = "Result value cannot be empty")
    private String resultValue; // 结果值，可以是字符串或数值

    private String unit; // 单位

    private String status; // 例如: "PENDING", "APPROVED", "REJECTED"

    private Map<String, Object> customFields; // 用于存储动态字段数据
}
