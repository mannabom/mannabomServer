package mannabom_server.manabom.infrastructure.external.kakao.exception;

public class KakaoApiException extends RuntimeException {

    public KakaoApiException(String message) {
        super(message);
    }

    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
