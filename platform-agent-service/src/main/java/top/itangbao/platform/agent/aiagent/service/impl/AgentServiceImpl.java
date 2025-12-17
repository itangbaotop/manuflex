package top.itangbao.platform.agent.aiagent.service.impl;

import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;
import top.itangbao.platform.agent.aiagent.service.*;
import top.itangbao.platform.agent.config.SecurityHeaderContext;
import top.itangbao.platform.agent.config.UserTokenCache;
import top.itangbao.platform.agent.dto.AgentResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    // 注入所有助理
    private final RouterAssistant routerAssistant;
    private final FormAssistant formAssistant;
    private final WorkflowAssistant workflowAssistant;
    private final DataAssistant dataAssistant;
    private final KnowledgeAssistant knowledgeAssistant;
    private final ChatAssistant chatAssistant;

    // 线程池用于执行路由分析，避免阻塞 Flux 订阅线程
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public Flux<String> executeTaskStream(String userInput, String tenantId, String userId) {
        Map<String, String> headers = new HashMap<>();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            safePut(headers, request, "Authorization");
            safePut(headers, request, "X-Auth-User");
            safePut(headers, request, "X-Auth-Roles");
            safePut(headers, request, "X-User-Dept-Id");
            safePut(headers, request, "X-User-Data-Scopes");
            safePut(headers, request, "X-User-Tenant-Id");
        }

        return Flux.create(emitter -> {
            // 在独立线程中运行，防止阻塞
            executor.submit(() -> {
                try {
                    UserTokenCache.put(userId, headers);
                    SecurityHeaderContext.set(headers);

                    log.info("🤖 [Router] 分析意图: {}", userInput);
                    // 1. 路由分类
                    AgentIntent intent = routerAssistant.classify(userInput);
                    log.info("🎯 [Router] 意图识别: {}", intent);

                    // 2. 分发给专家
                    TokenStream tokenStream;
                    switch (intent) {
                        case FORM -> tokenStream = formAssistant.chat(userInput, tenantId, userId);
                        case WORKFLOW -> tokenStream = workflowAssistant.chat(userInput, tenantId, userId);
                        case DATA -> tokenStream = dataAssistant.chat(userInput, tenantId, userId);
                        case KNOWLEDGE -> tokenStream = knowledgeAssistant.chat(userInput, tenantId, userId);
                        default -> tokenStream = chatAssistant.chat(userInput);
                    }

                    // 3. 桥接 TokenStream 到 Flux
                    tokenStream
                            .onPartialResponse(emitter::next)
                            .onCompleteResponse(response -> {
                                log.info("AI Stream 完成");
                                UserTokenCache.remove(userId);
                                emitter.complete();
                            })
                            .onError(error -> {
                                log.error("AI Stream Error", error);
                                UserTokenCache.remove(userId);
                                // 遇到上下文错误提示重置
                                if (error.getMessage() != null && error.getMessage().contains("INVALID_ARGUMENT")) {
                                    emitter.next("\n\n[系统: 上下文过长，请刷新页面重置会话]\n\n");
                                } else {
                                    emitter.next("\n\n[系统错误: " + error.getMessage() + "]\n\n");
                                }
                                emitter.complete();
                            })
                            .start();

                } catch (Exception e) {
                    log.error("Router 分发失败", e);
                    UserTokenCache.remove(userId);
                    emitter.error(e);
                } finally {
                    SecurityHeaderContext.clear();
                }
            });
        });
    }

    @Override
    public AgentResult executeTask(String userInput, String tenantId, String userId) {
        // 同步接口暂不实现路由，或者简单返回意图用于测试
        return AgentResult.success("Router Mode", "Use Stream API for full features");
    }

    private void safePut(Map<String, String> map, HttpServletRequest request, String key) {
        String value = request.getHeader(key);
        if (value != null) {
            map.put(key, value);
        }
    }
}