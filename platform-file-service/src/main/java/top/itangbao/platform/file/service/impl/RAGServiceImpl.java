package top.itangbao.platform.file.service.impl;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.itangbao.platform.file.dto.SearchResult;
import top.itangbao.platform.file.service.RAGService;
import top.itangbao.platform.file.service.VectorService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RAGServiceImpl implements RAGService {

    @Resource
    private VectorService vectorService;
    @Resource
    private ChatModel chatModel;

    @Override
    public String query(String question, String tenantId) {
        log.info("Querying knowledge base: question={}, tenantId={}", question, tenantId);
        
        List<SearchResult> searchResults = vectorService.search(question, 3);
        log.info("Search results count: {}", searchResults.size());
        
        // 按tenantId过滤结果
        List<SearchResult> filteredResults = searchResults.stream()
                .filter(result -> tenantId.equals(result.metadata.get("tenant_id")))
                .collect(Collectors.toList());
        
        log.info("Filtered results count: {}", filteredResults.size());

        if (filteredResults.isEmpty()) {
            return "抱歉，我在知识库中没有找到相关信息。";
        }

        String context = filteredResults.stream()
                .map(result -> (String) result.metadata.get("content"))
                .collect(Collectors.joining("\n\n"));
        
        log.info("Context length: {}", context.length());

        String prompt = String.format("""
                你是一个制造业知识助手。请根据以下知识库内容回答用户的问题。
                
                知识库内容：
                %s
                
                用户问题：%s
                
                请基于知识库内容给出准确、专业的回答。如果知识库中没有相关信息，请明确告知用户。
                """, context, question);

        return chatModel.chat(prompt);
    }
}
