package top.itangbao.platform.agent.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.agent.core.Agent;
import top.itangbao.platform.agent.core.AgentResult;
import top.itangbao.platform.agent.core.AgentTask;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.common.enums.FieldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单生成 Agent
 * 根据用户描述自动生成表单和数据模型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormAgent implements Agent {

    @Resource
    private ChatModel chatModel;
    @Resource
    private MetadataServiceFeignClient metadataServiceFeignClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getType() {
        return "FORM";
    }
    
    @Override
    public String getName() {
        return "表单生成助手";
    }
    
    @Override
    public String getDescription() {
        return "根据用户描述自动生成表单和数据模型";
    }
    
    @Override
    public boolean canHandle(AgentTask task) {
        return "FORM".equals(task.getTaskType()) || 
               task.getUserInput().contains("表单") || 
               task.getUserInput().contains("form");
    }
    
    @Override
    public AgentResult execute(AgentTask task) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("FormAgent executing task: {}", task.getTaskId());
            
            String prompt = buildFormGenerationPrompt(task.getUserInput());
            String response = chatModel.chat(prompt);
            
            // 解析AI响应并创建实际的表单
            Map<String, Object> formData = parseAndCreateForm(response, task);
            
            long executionTime = System.currentTimeMillis() - startTime;
            AgentResult result = AgentResult.success("表单生成完成", formData);
            result.setExecutionTime(executionTime);
            
            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("agentType", "FORM");
            metadata.put("formConfig", formData);
            result.setMetadata(metadata);
            
            return result;
            
        } catch (Exception e) {
            log.error("FormAgent execution failed", e);
            return AgentResult.failure("表单生成失败: " + e.getMessage());
        }
    }
    
    private String buildFormGenerationPrompt(String userInput) {
        return String.format("""
            你是一个专业的表单设计助手。请根据用户需求生成表单设计方案。
            
            用户需求：%s
            
            请按以下JSON格式返回表单设计：
            {
              "formName": "表单名称",
              "description": "表单描述", 
              "fields": [
                {
                  "name": "字段名",
                  "label": "字段标签",
                  "type": "字段类型(TEXT/NUMBER/DATE/SELECT等)",
                  "required": true/false,
                  "options": ["选项1", "选项2"] // 仅SELECT类型需要
                }
              ]
            }
            
            请确保返回有效的JSON格式。
            """, userInput);
    }
    
    private Map<String, Object> parseAndCreateForm(String aiResponse, AgentTask task) {
        try {
            // 尝试解析AI返回的JSON
            Map<String, Object> formConfig = objectMapper.readValue(aiResponse, Map.class);
            
            // 构建 metadata-service 所需的请求格式
            MetadataSchemaCreateRequest schemaRequest = MetadataSchemaCreateRequest.builder()
                .name((String) formConfig.get("formName"))
                .description((String) formConfig.get("description"))
                .tenantId(task.getTenantId())
                .fields(buildFieldRequests((List<Map<String, Object>>) formConfig.get("fields")))
                .build();
            
            // 调用 metadata-service 创建表单
            try {
                MetadataSchemaDTO createdSchema = metadataServiceFeignClient.createSchema(schemaRequest);
                formConfig.put("schemaId", createdSchema.getId());
                formConfig.put("created", true);
                log.info("Successfully created form schema: {}", createdSchema.getId());
            } catch (Exception e) {
                log.warn("Failed to create schema in metadata-service: {}", e.getMessage());
                formConfig.put("created", false);
                formConfig.put("error", e.getMessage());
            }
            
            return formConfig;
            
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            // 如果解析失败，返回原始响应
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("rawResponse", aiResponse);
            fallback.put("created", false);
            fallback.put("error", "AI响应格式解析失败");
            return fallback;
        }
    }
    
    private List<MetadataFieldCreateRequest> buildFieldRequests(List<Map<String, Object>> fields) throws JsonProcessingException {
        if (fields == null) return new ArrayList<>();
        
        List<MetadataFieldCreateRequest> fieldRequests = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            MetadataFieldCreateRequest fieldRequest = MetadataFieldCreateRequest.builder()
                .fieldName((String) field.get("name"))
                .fieldType(parseFieldType((String) field.get("type")))
                .required((Boolean) field.getOrDefault("required", false))
                .description((String) field.get("label"))
                .options(field.containsKey("options") ? objectMapper.writeValueAsString(field.get("options")) : null)
                .build();
            fieldRequests.add(fieldRequest);
        }
        return fieldRequests;
    }
    
    private FieldType parseFieldType(String type) {
        if (type == null) return FieldType.TEXT;
        return switch (type.toUpperCase()) {
            case "NUMBER" -> FieldType.NUMBER;
            case "DATE" -> FieldType.DATE;
            case "SELECT" -> FieldType.SELECT;
            case "BOOLEAN" -> FieldType.BOOLEAN;
            default -> FieldType.TEXT;
        };
    }
}