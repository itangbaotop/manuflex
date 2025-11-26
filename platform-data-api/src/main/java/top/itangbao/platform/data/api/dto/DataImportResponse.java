package top.itangbao.platform.data.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportResponse {
    private int totalRecords; // 总记录数
    private int successCount; // 成功导入的记录数
    private int failedCount; // 导入失败的记录数
    private String message; // 导入结果消息
    // 可以添加 List<Map<String, Object>> failedRecords; 来返回失败的详细信息
}
