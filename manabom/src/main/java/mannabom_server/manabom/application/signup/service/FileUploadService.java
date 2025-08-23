package mannabom_server.manabom.application.signup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 파일 업로드 서비스 - 로컬 테스트용
 */
@Service
@Slf4j
public class FileUploadService {

    @Value("${app.file.upload-dir:./uploads/}")
    private String uploadDir;

    @Value("${app.file.base-url:http://localhost:8080/files/}")
    private String baseUrl;

    // 지원 가능한 이미지 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 파일 업로드 (메인 메서드)
     */
    public String uploadFile(MultipartFile file, String category) {
        log.info("파일 업로드 시작 - 원본파일명: {}, 크기: {}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        try {
            // 1. 파일 검증
            validateFile(file);

            // 2. 저장 경로 생성
            String relativePath = generateFilePath(file, category);
            String fullPath = uploadDir + relativePath;

            // 3. 디렉토리 생성
            createDirectoryIfNotExists(Paths.get(fullPath).getParent().toString());

            // 4. 파일 저장
            Path targetPath = Paths.get(fullPath);
            Files.copy(file.getInputStream(), targetPath);

            log.info("파일 업로드 완료 - 저장경로: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("파일 업로드 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 파일 URL 생성
     */
    public String getFileUrl(String fileName) {
        return baseUrl + fileName;
    }

    /**
     * 파일 삭제
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir + fileName);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.debug("파일 삭제 완료 - 파일명: {}", fileName);
            }

            return deleted;
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 - 파일명: {}", fileName, e);
            return false;
        }
    }

    /**
     * 파일 검증
     */
    private void validateFile(MultipartFile file) {
        // 1. 빈 파일 체크
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        // 2. 파일 크기 체크
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다.", MAX_FILE_SIZE / 1024 / 1024)
            );
        }

        // 3. 파일 확장자 체크
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidImageFile(fileName)) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 파일 형식입니다. (%s만 허용)", String.join(", ", ALLOWED_EXTENSIONS))
            );
        }

        // 4. 실제 이미지 파일 검증 (보안 강화)
        if (!isValidImageContent(file)) {
            throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
        }
    }

    /**
     * 이미지 파일 확장자 검증
     */
    private boolean isValidImageFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * 실제 이미지 파일 검증
     * Content-Type과 실제 파일 내용 모두 검증
     */
    private boolean isValidImageContent(MultipartFile file) {
        try {
            // 1. Content-Type 확인 (기본 검증)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("잘못된 Content-Type: {}", contentType);
                return false;
            }

            // 2. 실제 이미지 파싱 검증 (보안 강화)
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                log.warn("이미지 파싱 실패 - 실제 이미지 파일이 아님");
                return false;
            }

            // 3. 이미지 기본 정보 검증
            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= 0 || height <= 0) {
                log.warn("유효하지 않은 이미지 크기: {}x{}", width, height);
                return false;
            }

            // 4. 너무 작은 이미지 거부 (프로필 사진 품질 보장)
            if (width < 100 || height < 100) {
                throw new IllegalArgumentException("이미지 크기는 최소 100x100 픽셀 이상이어야 합니다.");
            }

            log.debug("이미지 검증 완료 - 크기: {}x{}, 타입: {}", width, height, contentType);
            return true;

        } catch (IllegalArgumentException e) {
            // 비즈니스 로직 예외는 다시 throw
            throw e;
        } catch (Exception e) {
            log.warn("이미지 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 파일 저장 경로 생성
     */
    private String generateFilePath(MultipartFile file, String category) {
        // 날짜별 폴더 구조: category/2024/01/15/uuid.jpg
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        return String.format("%s/%s/%s", category, datePath, uniqueFileName);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex != -1 ? fileName.substring(lastDotIndex + 1) : "";
    }

    /**
     * 디렉토리 생성
     */
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