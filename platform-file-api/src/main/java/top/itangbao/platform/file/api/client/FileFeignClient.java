package top.itangbao.platform.file.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;

import java.util.Map;

@FeignClient(name = "platform-file-service", contextId = "fileClient")
public interface FileFeignClient {

    /**
     * 上传文件
     */
    @PostMapping(value = "/api/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileResponse upload(@RequestPart("file") MultipartFile file);

    /**
     * 获取文件信息
     */
    @GetMapping("/api/file/{objectName}/info")
    FileResponse getFileInfo(@PathVariable("objectName") String objectName);

    /**
     * 知识库 RAG 查询
     * 对应 KnowledgeController.query 方法
     */
    @PostMapping("/api/file/knowledge/query")
    Map<String, String> queryKnowledge(@RequestBody Map<String, String> request);

}