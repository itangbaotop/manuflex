package top.itangbao.platform.agent.aiagent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.itangbao.platform.file.api.client.FileFeignClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库检索工具
 * 赋予 AI 查阅企业文档和 SOP 的能力
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseTools {

    private final FileFeignClient fileFeignClient;

    @Tool("查询企业知识库、操作手册、SOP或历史文档。当用户询问具体业务规则、流程规范或'怎么做'的问题时使用。")
    public String searchKnowledgeBase(
            @dev.langchain4j.agent.tool.P("用户的问题或查询关键词") String question,
            @dev.langchain4j.agent.tool.P("租户ID") String tenantId) {

        log.info("🤖 AI正在查询知识库: {}", question);

        try {
            Map<String, String> request = new HashMap<>();
            request.put("question", question);
            request.put("tenantId", tenantId);

            // 调用 File Service 的 RAG 接口
            Map<String, String> response = fileFeignClient.queryKnowledge(request);

            String answer = response.get("answer");
            if (answer == null || answer.isBlank()) {
                return "知识库中未找到相关内容。";
            }
            return answer;

        } catch (Exception e) {
            log.error("知识库查询失败", e);
            return "查询知识库时发生错误: " + e.getMessage();
        }
    }
}