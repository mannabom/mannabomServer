package mannabom_server.manabom.application.auth.exception;

import java.util.List;

public class KakaoConsentRequiredException extends IllegalArgumentException {

    private final List<String> requiredScopes;

    // 회원번호는 토큰 주인이 카카오 API로 직접 조회할 수 있는 값이라, 테스트 계정 등록을 위해 메시지에 함께 보여준다.
    public KakaoConsentRequiredException(List<String> requiredScopes, String kakaoId) {
        super("카카오 계정의 필수 정보 제공 동의가 필요합니다: " + String.join(", ", requiredScopes)
                + " (회원번호 " + kakaoId + ")");
        this.requiredScopes = List.copyOf(requiredScopes);
    }

    public List<String> getRequiredScopes() {
        return requiredScopes;
    }
}
