package top.itangbao.platform.agent.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.itangbao.platform.agent.aiagent.service.ManuflexAssistant;
import top.itangbao.platform.agent.aiagent.tools.DataAnalysisTools;
import top.itangbao.platform.agent.aiagent.tools.KnowledgeBaseTools;
import top.itangbao.platform.agent.aiagent.tools.SchemaTools;
import top.itangbao.platform.agent.aiagent.tools.WorkflowTools;

@Configuration
public class AgentConfig {

    @Bean
    public ChatModel chatLanguageModel(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0) // 降低温度以提高工具调用的准确性
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean
    public ManuflexAssistant manuflexAssistant(
            ChatModel chatModel,
            SchemaTools schemaTools,
            WorkflowTools workflowTools,
            KnowledgeBaseTools knowledgeTools,
            DataAnalysisTools dataTools) { // 注入 DataTools

        return AiServices.builder(ManuflexAssistant.class)
                .chatModel(chatModel)
                .tools(schemaTools, workflowTools, knowledgeTools, dataTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(6)
                        .id(memoryId)
                        .build())
                .build();
    }
}