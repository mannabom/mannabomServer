package mannabom_server.manabom.presentation.common.exception;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.global.error.InvalidCursorException;
import mannabom_server.manabom.infrastructure.external.kakao.exception.KakaoApiException;
import mannabom_server.manabom.infrastructure.external.kakao.exception.KakaoAuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.InvalidClassException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기 - 회원가입 기본 기능용
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 입력값 검증 실패 (@Valid 어노테이션)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("입력값 검증 실패: {}", e.getMessage());

        Map<String, Object> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("입력값이 올바르지 않습니다.")
                .details(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 비즈니스 로직 예외 (회원가입 중복, 인증 실패 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("비즈니스 로직 오류: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(KakaoAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleKakaoAuthenticationException(KakaoAuthenticationException e) {
        log.warn("카카오 인증 실패: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ErrorResponse> handleKakaoApiException(KakaoApiException e) {
        log.error("카카오 API 연동 오류", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("카카오 로그인 서비스에 일시적인 오류가 발생했습니다.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("파일 크기가 10MB를 초과했습니다.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * 서버 내부 오류 (파일 업로드 실패, Redis 오류 등)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("서버 오류 발생", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("서버 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * DB 제약조건 위반 (중복 키, 외래키 제약 등)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("데이터 무결성 오류", e);

        String message = "데이터 처리 중 오류가 발생했습니다.";

        // 중복 키 에러 감지
        if (e.getMessage() != null && e.getMessage().contains("duplicate key")) {
            if (e.getMessage().contains("nick_name")) {
                message = "이미 사용 중인 닉네임입니다.";
            } else if (e.getMessage().contains("email")) {
                message = "이미 등록된 이메일입니다.";
            } else {
                message = "중복된 데이터가 있습니다.";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 잘못된 요청 파라미터 타입
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("잘못된 파라미터 타입: {} - {}", e.getName(), e.getValue());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(String.format("'%s' 파라미터의 값이 올바르지 않습니다.", e.getName()))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 필수 요청 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(String.format("'%s' 파라미터가 필요합니다.", e.getParameterName()))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 상태 이상
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        log.warn("상태 이상: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(String.format("'%s'", e.getMessage()))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidClassException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCursorException(InvalidCursorException e){
        log.warn("유효하지 않은 커서 접근 감지 : {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }


    /**
     * 에러 응답 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ErrorResponse {
        private boolean success;           // 항상 false
        private String message;            // 사용자에게 표시할 메시지
        private Map<String, Object> details; // 추가 정보 (선택적)
        private LocalDateTime timestamp;   // 오류 발생 시간
    }
}
