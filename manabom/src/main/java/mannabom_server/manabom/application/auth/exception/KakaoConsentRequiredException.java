package mannabom_server.manabom.application.auth.exception;

import java.util.List;

public class KakaoConsentRequiredException extends IllegalArgumentException {

    private final List<String> requiredScopes;

    public KakaoConsentRequiredException(List<String> requiredScopes) {
        super("카카오 계정의 필수 정보 제공 동의가 필요합니다: " + String.join(", ", requiredScopes));
        this.requiredScopes = List.copyOf(requiredScopes);
    }

    public List<String> getRequiredScopes() {
        return requiredScopes;
    }
}
