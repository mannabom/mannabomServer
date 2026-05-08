package mannabom_server.manabom.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "gcp")
@Slf4j
public class GcsFileUploadService implements FileStoragePort {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final Storage storage;
    private final String bucketName;
    private final String baseUrl;

    public GcsFileUploadService(
            Storage storage,
            @Value("${app.gcp.storage.bucket-name}") String bucketName,
            @Value("${app.gcp.storage.base-url}") String baseUrl
    ) {
        this.storage = storage;
        this.bucketName = bucketName;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public String uploadFile(MultipartFile file, String category) {
        log.info("GCS 파일 업로드 시작 - 원본파일명: {}, 크기: {}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        try {
            validateFile(file);

            String objectName = generateObjectName(file, category);
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            String fileUrl = baseUrl + objectName;
            log.info("GCS 파일 업로드 완료 - 버킷: {}, 객체: {}", bucketName, objectName);
            return fileUrl;
        } catch (Exception e) {
            log.error("GCS 파일 업로드 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String objectName = extractKeyFromUrl(fileUrl);
            if (objectName == null || objectName.isBlank()) {
                log.warn("URL에서 GCS 객체명을 추출할 수 없습니다: {}", fileUrl);
                return false;
            }

            boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
            log.debug("GCS 파일 삭제 결과 - 객체: {}, 삭제됨: {}", objectName, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("GCS 파일 삭제 실패 - URL: {}", fileUrl, e);
            return false;
        }
    }

    @Override
    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(baseUrl)) {
            return null;
        }
        return fileUrl.substring(baseUrl.length());
    }

    @Override
    public String presignedGetUrl(String key, Duration expires) {
        if (key == null || key.isBlank()) {
            return null;
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, key)).build();
        try {
            return storage.signUrl(
                    blobInfo,
                    expires.toMillis(),
                    TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.withV4Signature()
            ).toString();
        } catch (Exception e) {
            log.error("GCS signed URL 생성 실패 - 객체: {}", key, e);
            throw new RuntimeException("파일 접근 URL 생성에 실패했습니다.", e);
        }
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
                    String.format("지원하지 않는 파일 형식입니다. (%s만 허용)",
                            String.join(", ", ALLOWED_EXTENSIONS))
            );
        }

        if (!isValidImageContent(file)) {
            throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
        }
    }

    private boolean isValidImageFile(String fileName) {
        String extension = getFileExtension(fileName);
        return !extension.isEmpty() && ALLOWED_EXTENSIONS.contains(extension);
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

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("이미지 검증 중 오류 발생", e);
            return false;
        }
    }

    private String generateObjectName(MultipartFile file, String category) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString();

        if (!extension.isEmpty()) {
            uniqueFileName += "." + extension;
        } else {
            uniqueFileName += ".jpg";
        }

        return String.format("%s/%s/%s", category, datePath, uniqueFileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl;
        }
        return baseUrl + "/";
    }
}
