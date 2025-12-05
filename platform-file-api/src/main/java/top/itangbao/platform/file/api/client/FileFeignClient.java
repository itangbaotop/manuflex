package top.itangbao.platform.file.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;

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
}