package top.itangbao.platform.agent.aiagent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.data.api.client.DataServiceFeignClient;
import top.itangbao.platform.data.api.dto.DynamicDataResponse;
import top.itangbao.platform.data.api.dto.FilterRequestDTO;
import top.itangbao.platform.data.api.dto.PageRequestDTO;
import top.itangbao.platform.data.api.dto.PageResponseDTO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据分析工具
 * 赋予 AI 查询和统计业务数据的能力
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataAnalysisTools {

    private final DataServiceFeignClient dataClient;
    private final ObjectMapper objectMapper;

    /**
     * 通用数据查询工具
     */
    @Tool("查询业务数据。当用户问'有多少个...'、'查找...'、'列出...'时使用。")
    public String queryBusinessData(
            @dev.langchain4j.agent.tool.P("表单或模型名称 (Schema Name)，例如 'Car', 'Order'") String schemaName,
            @dev.langchain4j.agent.tool.P("查询过滤条件的JSON字符串 (Key-Value)，例如 '{\"status\": \"PENDING\", \"price.gt\": \"100\"}'") String filterJson,
            @dev.langchain4j.agent.tool.P("租户ID") String tenantId) {

        log.info("🤖 AI正在查询数据: schema={}, filter={}", schemaName, filterJson);

        try {
            // 1. 解析过滤条件
            Map<String, String> filters = new HashMap<>();
            if (filterJson != null && !filterJson.isBlank()) {
                try {
                    // 兼容处理：有时候 AI 会传 Map<String, Object>，我们需要转成 Map<String, String>
                    Map<String, Object> rawMap = objectMapper.readValue(filterJson, new TypeReference<Map<String, Object>>() {});
                    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                        filters.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                } catch (Exception e) {
                    return "查询失败：过滤条件格式错误，请使用标准的 JSON 对象格式。";
                }
            }

            // 2. 构建请求
            PageRequestDTO pageRequest = PageRequestDTO.builder().page(0).size(10).build(); // 默认只查前10条
            FilterRequestDTO filterRequest = FilterRequestDTO.builder().filters(filters).build();

            // 3. 调用 Data Service
            PageResponseDTO<DynamicDataResponse> response = dataClient.getAllDynamicData(
                    tenantId, schemaName, pageRequest, filterRequest
            );

            long total = response.getTotalElements();
            List<DynamicDataResponse> records = response.getContent();

            if (records.isEmpty()) {
                return "未找到符合条件的数据。";
            }

            // 4. 格式化返回结果给 AI
            // 提示：我们不直接返回 JSON，而是返回 AI 容易阅读的摘要，节省 Token
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("查询成功，共找到 %d 条记录。以下是前 %d 条数据摘要：\n", total, records.size()));

            for (DynamicDataResponse record : records) {
                sb.append(String.format("- [ID:%d] %s\n", record.getId(), formatDataMap(record.getData())));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("数据查询失败", e);
            // 尝试捕获 schema 不存在的错误
            if (e.getMessage().contains("not found")) {
                return "查询失败：未找到名为 '" + schemaName + "' 的数据模型，请确认表单名称是否正确。";
            }
            return "数据查询发生错误: " + e.getMessage();
        }
    }

    private String formatDataMap(Map<String, Object> data) {
        if (data == null) return "";
        return data.entrySet().stream()
                .limit(5) // 只展示前5个字段，避免 Token 爆炸
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}