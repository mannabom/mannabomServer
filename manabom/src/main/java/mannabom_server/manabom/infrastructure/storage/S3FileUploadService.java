package mannabom_server.manabom.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
@Slf4j
public class S3FileUploadService implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String baseUrl;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public S3FileUploadService(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.s3.bucket-name}") String bucketName,
            @Value("${app.aws.s3.access-key}") String accessKey,
            @Value("${app.aws.s3.secret-key}") String secretKey,
            @Value("${app.aws.s3.base-url}") String baseUrl
    ) {
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;

        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(creds)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(creds)
                .build();

        log.info("S3 파일 업로드 서비스 초기화 완료 - 버킷: {}, 리전: {}", bucketName, region);
    }

    @Override
    public String presignedGetUrl(String s3Key, Duration expires) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .responseCacheControl("max-age=31536000")
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(expires)
                .getObjectRequest(getReq)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }

    @Override
    public String uploadFile(MultipartFile file, String category) {
        log.info("S3 파일 업로드 시작 - 원본파일명: {}, 크기: {}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        try {
            validateFile(file);
            String s3Key = generateS3Key(file, category);
            uploadToS3(file, s3Key);

            String fileUrl = baseUrl + s3Key;
            log.info("S3 파일 업로드 완료 - S3 키: {}, URL: {}", s3Key, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String s3Key = extractKeyFromUrl(fileUrl);

            if (s3Key == null || s3Key.isEmpty()) {
                log.warn("URL에서 S3 키를 추출할 수 없습니다: {}", fileUrl);
                return false;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("S3 파일 삭제 완료 - S3 키: {}", s3Key);
            return true;
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 - URL: {}", fileUrl, e);
            return false;
        }
    }

    public String extractS3KeyFromUrl(String fileUrl) {
        return extractKeyFromUrl(fileUrl);
    }

    @Override
    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(baseUrl)) {
            return null;
        }
        return fileUrl.substring(baseUrl.length());
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

    private String generateS3Key(MultipartFile file, String category) {
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

    private void uploadToS3(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.debug("S3 업로드 완료 - 버킷: {}, 키: {}", bucketName, s3Key);
        } catch (Exception e) {
            log.error("S3 업로드 실패 - 키: {}", s3Key, e);
            throw new RuntimeException("S3 업로드에 실패했습니다.", e);
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

            log.debug("이미지 검증 완료 - 크기: {}x{}, 타입: {}", width, height, contentType);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("이미지 검증 중 오류 발생", e);
            return false;
        }
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
}
