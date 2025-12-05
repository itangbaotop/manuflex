package top.itangbao.platform.file.service;

import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.file.api.dto.FileResponse;

import java.io.InputStream;

public interface FileService {

    FileResponse upload(MultipartFile file);
    InputStream getFileStream(String objectName);
    void deleteFile(String objectName);
    FileResponse getFileInfo(String objectName);
}
