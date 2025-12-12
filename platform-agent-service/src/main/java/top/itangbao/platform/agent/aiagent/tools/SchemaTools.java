package top.itangbao.platform.agent.aiagent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.common.enums.FieldType;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import top.itangbao.platform.workflow.api.client.WorkflowServiceFeignClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤1：将业务逻辑封装为 Tool
 * 这是 AI 与你的业务系统（Metadata Service）交互的桥梁。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SchemaTools {

    private final MetadataServiceFeignClient metadataClient;
    private final WorkflowServiceFeignClient workflowClient;
    private final ObjectMapper objectMapper;

    /**
     * @Tool 注解是关键。
     * LLM 会读取这里的描述（Description）来决定是否调用这个方法。
     * 参数名和类型也会被 LLM 自动填充。
     */
    @Tool("根据用户描述创建业务表单、数据模型或实体。当用户想要'新建表单'、'设计模型'时使用此工具。")
    public String createFormSchema(
            @dev.langchain4j.agent.tool.P("表单名称...") String formName,
            @dev.langchain4j.agent.tool.P("描述...") String description,
            @dev.langchain4j.agent.tool.P("租户ID") String tenantId,
            @dev.langchain4j.agent.tool.P("字段定义...") String fieldsJson) {

        try {
            List<Map<String, Object>> fieldsList = objectMapper.readValue(fieldsJson, List.class);

            // 1. 创建数据模型 (Metadata)
            MetadataSchemaCreateRequest request = MetadataSchemaCreateRequest.builder()
                    .name(formName)
                    .description(description)
                    .tenantId(tenantId)
                    .fields(buildFieldRequests(fieldsList))
                    .build();

            MetadataSchemaDTO schema = metadataClient.createSchema(request);

            // 2. ✨ 自动生成 form-js UI Schema 并保存
            createUiFormDefinition(formName, description, fieldsList);

            return "✅ 成功创建数据模型和UI表单: " + schema.getName() + " (ID: " + schema.getId() + ")";

        } catch (Exception e) {
            log.error("操作失败", e);
            return "❌ 操作失败: " + e.getMessage();
        }
    }

    // 辅助方法：构建字段请求 (沿用你之前的逻辑)
    private List<MetadataFieldCreateRequest> buildFieldRequests(List<Map<String, Object>> fields) throws JsonProcessingException {
        List<MetadataFieldCreateRequest> requests = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            requests.add(MetadataFieldCreateRequest.builder()
                    .fieldName((String) field.get("name"))
                    .description((String) field.getOrDefault("label", field.get("name")))
                    .fieldType(parseFieldType((String) field.get("type")))
                    .required((Boolean) field.getOrDefault("required", false))
                    .options(field.containsKey("options") ? objectMapper.writeValueAsString(field.get("options")) : null)
                    .build());
        }
        return requests;
    }

    private FieldType parseFieldType(String type) {
        if (type == null) return FieldType.STRING;
        try {
            // 简单映射，你可以根据需要扩展
            if (type.equalsIgnoreCase("SELECT")) return FieldType.ENUM;
            return FieldType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FieldType.STRING;
        }
    }

    /**
     * 将字段列表转换为 form-js 的 JSON Schema
     */
    private void createUiFormDefinition(String key, String name, List<Map<String, Object>> fields) {
        try {
            Map<String, Object> formSchema = new HashMap<>();
            formSchema.put("schemaVersion", 4);
            formSchema.put("exporter", new HashMap<>());
            formSchema.put("type", "default");

            List<Map<String, Object>> components = new ArrayList<>();

            for (Map<String, Object> field : fields) {
                Map<String, Object> component = new HashMap<>();
                String fieldName = (String) field.get("name");
                String label = (String) field.getOrDefault("label", fieldName);
                String type = (String) field.get("type");
                boolean required = (Boolean) field.getOrDefault("required", false);

                component.put("key", fieldName);
                component.put("label", label);
                component.put("validate", Map.of("required", required));

                // 类型映射：Metadata Type -> Form-js Component Type
                switch (type.toUpperCase()) {
                    case "NUMBER":
                    case "INTEGER":
                        component.put("type", "number");
                        break;
                    case "DATE":
                    case "DATETIME":
                        component.put("type", "datetime"); // form-js 支持 datetime
                        break;
                    case "BOOLEAN":
                        component.put("type", "checkbox");
                        break;
                    case "SELECT": // 假设 AI 传了 SELECT
                    case "ENUM":
                        component.put("type", "select");
                        // 处理选项
                        if (field.containsKey("options")) {
                            List<String> opts = (List<String>) field.get("options");
                            List<Map<String, String>> values = new ArrayList<>();
                            for (String opt : opts) {
                                values.add(Map.of("label", opt, "value", opt));
                            }
                            component.put("values", values);
                        }
                        break;
                    case "TEXT": // 长文本
                        component.put("type", "textarea");
                        break;
                    default: // STRING
                        component.put("type", "textfield");
                }
                components.add(component);
            }

            formSchema.put("components", components);

            // 调用 Workflow Service 保存
            Map<String, Object> request = new HashMap<>();
            request.put("key", key + "_form"); // 加个后缀区分
            request.put("name", name);
            request.put("schema", formSchema); // 直接传 Map，Jackson 会转 JSON

            workflowClient.createFormDefinition(request);
            log.info("Auto-generated UI form for {}", key);

        } catch (Exception e) {
            log.warn("Failed to auto-generate UI form", e);
            // UI生成失败不应阻断主流程
        }
    }
}