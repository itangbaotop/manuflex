package top.itangbao.platform.file.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileResponse {
    private String fileName;      // 原始文件名
    private String objectName;    // MinIO 中的存储名 (UUID)
    private String url;           // 访问链接 (HTTP)
    private String contentType;   // 文件类型
    private Long size;            // 文件大小
}