package mannabom_server.manabom.application.common.port;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

public interface FileStoragePort {
    String uploadFile(MultipartFile file, String category);

    boolean deleteFile(String fileUrl);

    String extractKeyFromUrl(String fileUrl);

    String presignedGetUrl(String key, Duration expires);
}
