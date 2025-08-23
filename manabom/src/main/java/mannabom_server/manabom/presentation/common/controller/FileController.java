package mannabom_server.manabom.presentation.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 파일 다운로드 컨트롤러
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    @Value("${app.file.upload-dir:./uploads/}")
    private String uploadDir;

    /**
     * 파일 다운로드
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> downloadFile(@RequestParam String file) {
        try {
            log.debug("파일 다운로드 요청 - 파일명: {}", file);

            Path filePath = Paths.get(uploadDir).resolve(file).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 찾을 수 없습니다 - 파일명: {}", file);
                return ResponseEntity.notFound().build();
            }

            // 파일 확장자에 따른 Content-Type 설정
            String contentType = determineContentType(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("파일 다운로드 실패 - 파일명: {}", file, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 파일 확장자에 따른 Content-Type 결정
     */
    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }
}