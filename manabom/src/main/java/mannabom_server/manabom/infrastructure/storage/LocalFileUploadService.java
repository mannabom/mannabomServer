package mannabom_server.manabom.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileUploadService implements FileStoragePort {

    @Value("${app.file.upload-dir:./uploads/}")
    private String uploadDir;

    @Value("${app.file.base-url:http://localhost:8080/files/}")
    private String baseUrl;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public String uploadFile(MultipartFile file, String category) {
        log.info("파일 업로드 시작 - 원본파일명: {}, 크기: {}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        try {
            validateFile(file);

            String relativePath = generateFilePath(file, category);
            String fullPath = uploadDir + relativePath;

            createDirectoryIfNotExists(Paths.get(fullPath).getParent().toString());

            Path targetPath = Paths.get(fullPath);
            Files.copy(file.getInputStream(), targetPath);

            log.info("파일 업로드 완료 - 저장경로: {}", relativePath);
            return getFileUrl(relativePath);
        } catch (IOException e) {
            log.error("파일 업로드 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    public String getFileUrl(String fileName) {
        return baseUrl + fileName;
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String fileName = extractKeyFromUrl(fileUrl);
            if (fileName == null || fileName.isBlank()) {
                return false;
            }
            Path filePath = Paths.get(uploadDir + fileName);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.debug("파일 삭제 완료 - 파일명: {}", fileName);
            }

            return deleted;
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 - URL: {}", fileUrl, e);
            return false;
        }
    }

    @Override
    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null) {
            return null;
        }
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }
        return fileUrl;
    }

    @Override
    public String presignedGetUrl(String key, Duration expires) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return getFileUrl(key);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다.", MAX_FILE_SIZE / 1024 / 1024)
            );
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidImageFile(fileName)) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 파일 형식입니다. (%s만 허용)", String.join(", ", ALLOWED_EXTENSIONS))
            );
        }

        if (!isValidImageContent(file)) {
            throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
        }
    }

    private boolean isValidImageFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private boolean isValidImageContent(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("잘못된 Content-Type: {}", contentType);
                return false;
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                log.warn("이미지 파싱 실패 - 실제 이미지 파일이 아님");
                return false;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= 0 || height <= 0) {
                log.warn("유효하지 않은 이미지 크기: {}x{}", width, height);
                return false;
            }

            if (width < 100 || height < 100) {
                throw new IllegalArgumentException("이미지 크기는 최소 100x100 픽셀 이상이어야 합니다.");
            }

            log.debug("이미지 검증 완료 - 크기: {}x{}, 타입: {}", width, height, contentType);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("이미지 검증 중 오류 발생", e);
            return false;
        }
    }

    private String generateFilePath(MultipartFile file, String category) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString();
        if (!extension.isEmpty()) {
            uniqueFileName += "." + extension;
        }

        return String.format("%s/%s/%s", category, datePath, uniqueFileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex != -1 ? fileName.substring(lastDotIndex + 1) : "";
    }

    private void createDirectoryIfNotExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.debug("디렉토리 생성 완료: {}", dirPath);
            } else {
                log.error("디렉토리 생성 실패: {}", dirPath);
                throw new RuntimeException("디렉토리 생성에 실패했습니다: " + dirPath);
            }
        }
    }
}
