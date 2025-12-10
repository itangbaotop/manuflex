package top.itangbao.platform.file.service;

import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.domain.KnowledgeDocument;

import java.util.*;

public interface KnowledgeService {

    KnowledgeDocument uploadDocument(MultipartFile file, String tenantId, String category, String createdBy) throws Exception;

    List<KnowledgeDocument> listDocuments(String tenantId);

    void deleteDocument(Long id);
}