package mannabom_server.manabom.infrastructure.external.kakao;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.infrastructure.external.kakao.exception.KakaoApiException;
import mannabom_server.manabom.infrastructure.external.kakao.exception.KakaoAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * 카카오 액세스 토큰 검증 및 사용자 정보 조회 서비스.
 */
@Service
@Slf4j
public class KakaoApiService {

    private static final String KAKAO_TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final long appId;

    public KakaoApiService(@Value("${app.kakao.app-id}") long appId) {
        this.appId = appId;
        this.webClient = WebClient.builder().build();
        log.info("카카오 API 서비스 초기화 완료");
    }

    /**
     * 토큰이 만나봄 카카오 앱에서 발급됐는지 검증한 뒤 사용자 정보를 조회한다.
     */
    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        validateAccessToken(accessToken);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(MAP_RESPONSE_TYPE)
                    .block();

            if (response == null) {
                throw new KakaoApiException("카카오 사용자 정보 응답이 비어 있습니다.");
            }

            return response;
        } catch (KakaoAuthenticationException | KakaoApiException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw mapKakaoApiException("사용자 정보 조회", e);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new KakaoApiException("카카오 사용자 정보를 조회할 수 없습니다.", e);
        }
    }

    private void validateAccessToken(String accessToken) {
        try {
            Map<String, Object> tokenInfo = webClient.get()
                    .uri(KAKAO_TOKEN_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(MAP_RESPONSE_TYPE)
                    .block();

            validateTokenInfo(tokenInfo);
        } catch (KakaoAuthenticationException | KakaoApiException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw mapKakaoApiException("액세스 토큰 검증", e);
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 검증 실패", e);
            throw new KakaoApiException("카카오 액세스 토큰을 검증할 수 없습니다.", e);
        }
    }

    void validateTokenInfo(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            throw new KakaoApiException("카카오 액세스 토큰 정보 응답이 비어 있습니다.");
        }

        Object issuedAppIdValue = tokenInfo.get("app_id");
        if (!(issuedAppIdValue instanceof Number issuedAppId)) {
            throw new KakaoAuthenticationException("카카오 앱 정보를 확인할 수 없는 토큰입니다.");
        }

        if (issuedAppId.longValue() != appId) {
            log.warn("다른 카카오 앱에서 발급된 액세스 토큰이 거부되었습니다. issuedAppId={}",
                    issuedAppId.longValue());
            throw new KakaoAuthenticationException("허용되지 않은 카카오 앱에서 발급된 토큰입니다.");
        }
    }

    private RuntimeException mapKakaoApiException(String operation, WebClientResponseException e) {
        if (e.getStatusCode().value() == 401) {
            log.warn("카카오 {} 실패: 유효하지 않거나 만료된 토큰", operation);
            return new KakaoAuthenticationException("유효하지 않거나 만료된 카카오 토큰입니다.", e);
        }

        log.error("카카오 {} 실패 - status={}", operation, e.getStatusCode().value(), e);
        return new KakaoApiException("카카오 서비스 요청에 실패했습니다.", e);
    }
}
