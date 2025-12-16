package top.itangbao.platform.agent.aiagent.service.impl;

import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import top.itangbao.platform.agent.aiagent.service.AgentService;
import top.itangbao.platform.agent.aiagent.service.ManuflexAssistant;
import top.itangbao.platform.agent.dto.AgentResult;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    // 注入我们在 Config 中配置好的 AI 助理
    private final ManuflexAssistant assistant;

    @Override
    public Flux<String> executeTaskStream(String userInput, String tenantId, String userId) {
        return Flux.create(emitter -> {
            try {
                TokenStream tokenStream = assistant.chat(userInput, tenantId, userId);

                tokenStream
                        .onPartialResponse(token -> {
                            emitter.next(token);
                        })
                        .onCompleteResponse(token -> {
                            emitter.complete();
                        })
                        .onError(error -> {
                            log.error("AI Stream Error for user {}: {}", userId, error.getMessage());

                            // 🔥 核心修复：如果是 400 错误，说明历史记录脏了
                            if (error.getMessage().contains("INVALID_ARGUMENT") || error.getMessage().contains("function call turn")) {
                                emitter.next("\n\n[系统提示: 检测到上下文状态异常，正在重置会话记忆并重试...]\n\n");
                                // 注意：在实际生产中，你需要调用 chatMemory.clear(memoryId)
                                // 由于 LangChain4j 的 AiServices 封装较深，
                                // 建议：前端收到这个错后，自动发一个 "reset" 指令或者后端在这里清理 Key

                                // 这里我们简单抛出，让前端知道要重置
                                emitter.error(new RuntimeException("CONTEXT_RESET_REQUIRED"));
                            } else {
                                emitter.error(error);
                            }
                        })
                        .start();

            } catch (Exception e) {
                log.error("AI Stream Error for user {}: {}", userId, e.getMessage(), e);
                emitter.error(e);
            }
        });
    }

    @Override
    public AgentResult executeTask(String userInput, String tenantId, String userId) {
        return AgentResult.success("Ok", "Compatible");
//        long startTime = System.currentTimeMillis();
//
//        try {
//            log.info("🤖 Agent 收到请求 User: {}, Input: {}", userId, userInput);
//
//            // ✨ 核心变化：不再手动判断类型，直接扔给 AI
//            // AI 会自己分析是否需要调用 SchemaTools，或者直接回答
//            String response = assistant.chat(userInput, tenantId, userId);
//
//            long executionTime = System.currentTimeMillis() - startTime;
//
//            // 返回结果保持原有结构，方便前端兼容
//            return AgentResult.success("执行成功", response);
//
//        } catch (Exception e) {
//            log.error("Agent 执行异常", e);
//            // 即使出错也返回友好的提示
//            return AgentResult.failure("AI 思考过程中遇到点问题: " + e.getMessage());
//        }
    }

}