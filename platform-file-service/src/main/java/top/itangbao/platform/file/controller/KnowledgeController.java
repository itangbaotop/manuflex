package top.itangbao.platform.file.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.common.util.SecurityUtils;
import top.itangbao.platform.file.domain.KnowledgeDocument;
import top.itangbao.platform.file.service.KnowledgeService;
import top.itangbao.platform.file.service.RAGService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/file/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;
    @Resource
    private RAGService ragService;
    
    @PostMapping("/upload")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KnowledgeDocument> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "category", defaultValue = "其他") String category) {
        try {
            String username = SecurityUtils.getUsername();
            if (username == null) {
                username = "system"; // 默认用户名
            }
            log.info("用户名：{}", username);
            KnowledgeDocument document = knowledgeService.uploadDocument(file, tenantId, category, username);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Failed to upload knowledge document", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeDocument>> listDocuments(@RequestParam("tenantId") String tenantId) {
        return ResponseEntity.ok(knowledgeService.listDocuments(tenantId));
    }
    
    @DeleteMapping("/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/query")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> query(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String tenantId = request.get("tenantId");
        String answer = ragService.query(question, tenantId);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}