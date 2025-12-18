package top.itangbao.platform.agent.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.itangbao.platform.agent.aiagent.service.*;
import top.itangbao.platform.agent.aiagent.tools.*;

@Configuration
public class AgentConfig {

    @Value("${ai.provider:gemini}")
    private String provider;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openAiModel;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;


    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20) // 设置记忆窗口大小
                .build();
    }


    @Bean
    public ChatModel routerModel() {
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .baseUrl(openAiBaseUrl)
                    .modelName(openAiModel)
                    .temperature(0.0) // 保持低温度以提高工具调用准确性
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName(geminiModel)
                    .temperature(0.0)
                    .logRequestsAndResponses(true)
                    .build();
        }
    }

    // 2. 定义流式模型给专家 Agent 用 (体验好)
    @Bean
    public StreamingChatModel chatLanguageModel() {
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiStreamingChatModel.builder()
                    .apiKey(openAiApiKey)
                    .baseUrl(openAiBaseUrl)
                    .modelName(openAiModel)
                    .temperature(0.0) // 保持低温度以提高工具调用准确性
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            return GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName(geminiModel)
                    .temperature(0.0)
                    .logRequestsAndResponses(true)
                    .build();
        }
    }

    // --- 注册各个 Assistant ---

    @Bean
    public RouterAssistant routerAssistant(ChatModel routerModel) {
        return AiServices.builder(RouterAssistant.class)
                .chatModel(routerModel)
                .build();
    }

    @Bean
    public FormAssistant formAssistant(StreamingChatModel streamingChatModel, SchemaTools schemaTools) {
        return AiServices.builder(FormAssistant.class)
                .streamingChatModel(streamingChatModel)
                .tools(schemaTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public WorkflowAssistant workflowAssistant(StreamingChatModel streamingChatModel, WorkflowTools workflowTools) {
        return AiServices.builder(WorkflowAssistant.class)
                .streamingChatModel(streamingChatModel)
                .tools(workflowTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public DataAssistant dataAssistant(StreamingChatModel streamingChatModel, DataAnalysisTools dataTools) {
        return AiServices.builder(DataAssistant.class)
                .streamingChatModel(streamingChatModel)
                .tools(dataTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public KnowledgeAssistant knowledgeAssistant(StreamingChatModel streamingChatModel, KnowledgeBaseTools knowledgeTools) {
        return AiServices.builder(KnowledgeAssistant.class)
                .streamingChatModel(streamingChatModel)
                .tools(knowledgeTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public ChatAssistant chatAssistant(StreamingChatModel streamingChatModel,
                                       ChatMemoryProvider chatMemoryProvider,
                                       // 注入所有工具 Bean
                                       SchemaTools schemaTools,
                                       WorkflowTools workflowTools,
                                       DataAnalysisTools dataTools,
                                       KnowledgeBaseTools knowledgeTools) {
        return AiServices.builder(ChatAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(schemaTools, workflowTools, dataTools, knowledgeTools)
                .build();
    }
}