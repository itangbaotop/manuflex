package top.itangbao.platform.file.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;
import top.itangbao.platform.file.service.impl.FileServiceImpl;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired private FileServiceImpl fileService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('data:create', 'ROLE_USER')")
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.upload(file));
    }

    /**
     * 预览/下载文件 (代理模式)
     * URL: GET /api/file/view/{objectName}
     */
    @GetMapping("/view/{objectName}")
    public void viewFile(
            @PathVariable String objectName,
            @RequestParam(required = false) Boolean download,
            HttpServletResponse response) {

        try (InputStream stream = fileService.getFileStream(objectName)) {
            FileResponse info = fileService.getFileInfo(objectName);

            // 1. 设置 Content-Type (图片/PDF等)
            response.setContentType(info.getContentType());

            // 2. 设置 Content-Disposition (决定是预览还是下载)
            String disposition = (download != null && download) ? "attachment" : "inline";

            // 3. 处理中文文件名乱码 (URL编码)
            String encodedFileName = URLEncoder.encode(info.getFileName(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            response.setHeader("Content-Disposition",
                    disposition + "; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

            // 4. 写出流
            IOUtils.copy(stream, response.getOutputStream());
            response.flushBuffer();

        } catch (Exception e) {
            // 如果文件不存在或出错，返回 404
            response.setStatus(404);
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{objectName}")
    @PreAuthorize("hasAnyAuthority('data:create', 'ROLE_USER')")
    public ResponseEntity<Void> deleteFile(@PathVariable String objectName) {
        fileService.deleteFile(objectName);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{objectName}/info")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<FileResponse> getFileInfo(@PathVariable String objectName) {
        return ResponseEntity.ok(fileService.getFileInfo(objectName));
    }
}