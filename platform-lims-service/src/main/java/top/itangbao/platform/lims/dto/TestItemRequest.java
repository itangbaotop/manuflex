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
public class TestItemRequest {
    @NotBlank(message = "Test item name cannot be empty")
    private String itemName;

    @NotBlank(message = "Method reference cannot be empty")
    private String methodReference; // 例如: "ASTM D1234"

    @NotNull(message = "Price cannot be null")
    private Double price;

    private String description;

    private Map<String, Object> customFields; // 用于存储动态字段数据
}
