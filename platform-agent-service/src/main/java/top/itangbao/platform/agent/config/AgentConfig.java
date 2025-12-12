package top.itangbao.platform.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置
 */
@Configuration
public class AgentConfig {
    
    @Bean
    public ChatModel chatLanguageModel(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .build();
    }
}