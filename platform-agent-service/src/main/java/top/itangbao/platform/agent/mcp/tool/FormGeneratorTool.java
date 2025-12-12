package top.itangbao.platform.agent.mcp.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.itangbao.platform.agent.aiagent.agents.FormAgent;
import top.itangbao.platform.agent.core.AgentTask;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FormGeneratorTool implements McpTool {
    
    private final FormAgent formAgent;
    
    @Override
    public String getName() {
        return "form_generator";
    }
    
    @Override
    public String getDescription() {
        return "根据描述自动生成表单和数据模型";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "description", Map.of(
                    "type", "string",
                    "description", "表单需求描述"
                ),
                "tenantId", Map.of(
                    "type", "string", 
                    "description", "租户ID"
                )
            ),
            "required", new String[]{"description", "tenantId"}
        );
    }
    
    @Override
    public Object execute(Map<String, Object> arguments) {
        String description = (String) arguments.get("description");
        String tenantId = (String) arguments.get("tenantId");
        
        AgentTask task = AgentTask.builder()
            .taskType("FORM")
            .userInput(description)
            .tenantId(tenantId)
            .build();
            
        return formAgent.execute(task);
    }
}