package top.itangbao.platform.agent.aiagent.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    public AgentResult executeTask(String userInput, String tenantId, String userId) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🤖 Agent 收到请求 User: {}, Input: {}", userId, userInput);

            // ✨ 核心变化：不再手动判断类型，直接扔给 AI
            // AI 会自己分析是否需要调用 SchemaTools，或者直接回答
            String response = assistant.chat(userInput, tenantId, userId);

            long executionTime = System.currentTimeMillis() - startTime;

            // 返回结果保持原有结构，方便前端兼容
            return AgentResult.success("执行成功", response);

        } catch (Exception e) {
            log.error("Agent 执行异常", e);
            // 即使出错也返回友好的提示
            return AgentResult.failure("AI 思考过程中遇到点问题: " + e.getMessage());
        }
    }

}