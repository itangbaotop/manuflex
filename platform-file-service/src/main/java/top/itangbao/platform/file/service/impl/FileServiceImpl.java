package top.itangbao.platform.file.service.impl;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;
import top.itangbao.platform.file.config.MinioConfig;
import top.itangbao.platform.file.service.FileService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired private MinioClient minioClient;
    @Autowired private MinioConfig minioConfig;

    @Value("${file.gateway-url:http://localhost:8080}")
    private String gatewayUrl;

    /**
     * 上传文件
     */
    @Override
    public FileResponse upload(MultipartFile file) {
        try {
            // 1. 确保 Bucket 存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
            }

            // 2. 生成存储名 (UUID) 和 原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "unknown";

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
            String objectName = UUID.randomUUID().toString() + extension;

            // 3. 准备元数据 (User Metadata) 存储原始文件名
            // MinIO 要求 metadata 的 key 是字符串，value 也是字符串
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("original-filename", originalFilename);

            // 4. 上传 (带上 metadata)
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .userMetadata(userMetadata)
                            .build());

            // 例如: http://localhost:8080/api/file/view/abcd-1234.jpg
            // 这样前端访问的是我们的 Controller，而不是 MinIO
            String finalUrl = gatewayUrl + "/api/file/view/" + objectName;

            return FileResponse.builder()
                    .fileName(originalFilename)
                    .objectName(objectName)
                    .url(finalUrl)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();

        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    @Override
    public InputStream getFileStream(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get file stream: {}", objectName, e);
            throw new RuntimeException("File not found or inaccessible");
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            throw new RuntimeException("Delete file failed");
        }
    }

    @Override
    public FileResponse getFileInfo(String objectName) {
        try {
            // 获取对象元数据
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build());

            // MinIO SDK 获取的 userMetadata key 可能会变成小写，建议都按小写取
            String originalFilename = stat.userMetadata().get("original-filename");
            if (originalFilename == null) {
                originalFilename = objectName; // 兜底：如果没有存元数据，就用 objectName
            }

            String proxyUrl = gatewayUrl + "/api/file/view/" + objectName;

            return FileResponse.builder()
                    .fileName(originalFilename)
                    .objectName(objectName)
                    .size(stat.size())
                    .contentType(stat.contentType())
                    .url(proxyUrl)
                    .build();
        } catch (Exception e) {
            log.error("Get file info failed: {}", objectName, e);
            throw new RuntimeException("File info not found");
        }
    }
}