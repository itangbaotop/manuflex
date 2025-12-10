package top.itangbao.platform.file.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;
import top.itangbao.platform.file.config.MinioConfig;
import top.itangbao.platform.file.domain.KnowledgeDocument;
import top.itangbao.platform.file.repository.KnowledgeDocumentRepository;
import top.itangbao.platform.file.service.FileService;
import top.itangbao.platform.file.service.KnowledgeService;
import top.itangbao.platform.file.service.VectorService;

import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeDocumentRepository documentRepository;
    @Resource
    private VectorService vectorService;
    @Resource
    private FileService fileService;
    @Autowired
    private MinioClient minioClient;
    @Autowired private MinioConfig minioConfig;

    private Tika tika = new Tika();

    @Value("${document.chunk-size:500}")
    private int chunkSize;

    @Value("${document.chunk-overlap:50}")
    private int chunkOverlap;

    @Override
    public KnowledgeDocument uploadDocument(MultipartFile file, String tenantId, String category, String createdBy) throws Exception {
        // 1. 上传文件到 MinIO
        FileResponse upload = fileService.upload(file);
        String fileUrl = upload.getUrl();

        // 2. 解析文档内容
//        String content = tika.parseToString(file.getInputStream());

        // 3. 保存文档元数据
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setFileUrl(fileUrl);
//        document.setContent(content);
        document.setTenantId(tenantId);
        document.setCategory(category);
        document.setCreatedBy(createdBy);
        document.setVectorStatus("PENDING");

        document = documentRepository.save(document);

        // 4. 异步处理向量化
        processDocumentAsync(document);

        return document;
    }

    private void processDocumentAsync(KnowledgeDocument document) {
        new Thread(() -> {
            try {
                document.setVectorStatus("PROCESSING");
                documentRepository.save(document);

                // 直接从MinIO获取文件内容，而不是通过HTTP URL
                String objectName = extractObjectNameFromUrl(document.getFileUrl());

                GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .build();
                
                String content = tika.parseToString(minioClient.getObject(getObjectArgs));

                List<String> chunks = splitText(content);
                document.setChunkCount(chunks.size());

                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("doc_id", document.getId().toString());
                    metadata.put("chunk_index", String.valueOf(i));
                    metadata.put("title", document.getTitle());
                    metadata.put("category", document.getCategory());
                    metadata.put("tenant_id", document.getTenantId());
                    metadata.put("content", chunks.get(i));

                    long pointId = document.getId() * 10000 + i;
                    String result = vectorService.upsertDocument(pointId, chunks.get(i), metadata);
                    log.info("Chunk {} upsert result: {}", i + 1, result);
                }

                document.setVectorStatus("COMPLETED");
                documentRepository.save(document);
                log.info("Document processed successfully: {}", document.getId());
            } catch (Exception e) {
                document.setVectorStatus("FAILED");
                documentRepository.save(document);
                log.error("Failed to process document ID: {}, URL: {}", document.getId(), document.getFileUrl(), e);
            }
        }).start();
    }

    private String extractObjectNameFromUrl(String fileUrl) {
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }
    
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - chunkOverlap;
        }

        return chunks;
    }

    @Override
    public List<KnowledgeDocument> listDocuments(String tenantId) {
        return documentRepository.findByTenantId(tenantId);
    }

    @Override
    public void deleteDocument(Long id) {
        KnowledgeDocument document = documentRepository.findById(id).orElse(null);
        if (document != null) {
            // 删除 MinIO 文件 (复用现有 FileService)
            try {
                if (document.getFileUrl() != null) {
                    fileService.deleteFile(document.getFileUrl());
                }
            } catch (Exception e) {
                log.error("Failed to delete file from MinIO", e);
            }
            documentRepository.deleteById(id);
        }
    }
}