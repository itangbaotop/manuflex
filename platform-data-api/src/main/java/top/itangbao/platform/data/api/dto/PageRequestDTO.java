package top.itangbao.platform.data.api.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequestDTO {
    @Min(value = 0, message = "Page number must be greater than or equal to 0")
    private int page = 0; // 当前页码，从 0 开始

    @Min(value = 1, message = "Page size must be greater than or equal to 1")
    private int size = 10; // 每页大小

    private String sortBy; // 排序字段
    private String sortOrder; // 排序顺序 (asc/desc)
}
