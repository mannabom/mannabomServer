package mannabom_server.manabom.infrastructure.external.kakao;

import mannabom_server.manabom.infrastructure.external.kakao.exception.KakaoAuthenticationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoApiServiceTest {

    private final KakaoApiService kakaoApiService = new KakaoApiService(1234L);

    @Test
    void acceptsTokenIssuedByConfiguredKakaoApp() {
        assertThatCode(() -> kakaoApiService.validateTokenInfo(Map.of("app_id", 1234)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTokenIssuedByAnotherKakaoApp() {
        assertThatThrownBy(() -> kakaoApiService.validateTokenInfo(Map.of("app_id", 9999)))
                .isInstanceOf(KakaoAuthenticationException.class)
                .hasMessageContaining("허용되지 않은 카카오 앱");
    }

    @Test
    void rejectsTokenWithoutAppId() {
        assertThatThrownBy(() -> kakaoApiService.validateTokenInfo(Map.of("id", 98765)))
                .isInstanceOf(KakaoAuthenticationException.class)
                .hasMessageContaining("카카오 앱 정보");
    }
}
