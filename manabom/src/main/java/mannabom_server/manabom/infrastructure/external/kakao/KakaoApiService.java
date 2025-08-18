package mannabom_server.manabom.infrastructure.external.kakao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 카카오 OAuth API 연동 서비스
 *
 * 주요 기능:
 * 1. 인증 코드 → access token 발급
 * 2. access token → 사용자 정보 조회
 *
 * 카카오 API 문서: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
 */
@Service
@Slf4j
public class KakaoApiService {

    // 카카오 OAuth 토큰 발급 엔드포인트
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    // 카카오 사용자 정보 조회 엔드포인트
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";


    private final WebClient webClient;
    private final String clientId;

    public KakaoApiService(
            @Value("${app.kakao.client-id}") String clientId
    ) {
        this.clientId = clientId;
        this.webClient = WebClient.builder().build();

        log.info("카카오 API 서비스 초기화 완료");
    }

    /**
     * 카카오 OAuth 토큰 발급
     * 인증 코드를 받아서 access token 발급
     */
    public Map<String, Object> getKakaoToken(String authorizationCode, String redirectUri) {
        log.info("카카오 토큰 발급 요청 시작 - redirectUri: {}", redirectUri);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");     // OAuth 2.0 grant type
        params.add("client_id", clientId);                  // 카카오 앱 키
        params.add("redirect_uri", redirectUri);            // 등록된 리다이렉트 URI
        params.add("code", authorizationCode);              // 프론트엔드에서 받은 인증 코드

        try {
            // 카카오 서버에 POST 요청 전송
            Map<String, Object> response = webClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(params))   // form-urlencoded 형태로 전송
                    .retrieve()
                    .bodyToMono(Map.class)                      // JSON 응답을 Map으로 변환
                    .block();                                   // 동기적으로 결과 대기

            log.info("카카오 토큰 발급 성공");
            return response;
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패", e);
            throw new RuntimeException("카카오 토큰 발급에 실패했습니다.", e);
        }
    }

    /**
     * 카카오 사용자 정보 조회
     * access token 사용해서 사용자 정보 가져오기
     */
    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 조회 요청");

        try {
            Map<String, Object> response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("카카오 사용자 정보 조회 성공");
            return response;

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }
}
