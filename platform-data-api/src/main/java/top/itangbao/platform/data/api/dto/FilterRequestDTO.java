package top.itangbao.platform.data.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterRequestDTO {
    private Map<String, String> filters; // 过滤条件，例如: {"fieldName": "value", "fieldName.gt": "value"}
    // 键的格式可以是 "fieldName" (等于), "fieldName.gt" (大于), "fieldName.lt" (小于), "fieldName.like" (模糊匹配) 等
}
