package mannabom_server.manabom.presentation.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.auth.dto.request.KakaoLoginRequestDto;
import mannabom_server.manabom.application.auth.dto.request.RefreshTokenRequestDto;
import mannabom_server.manabom.application.auth.dto.response.KakaoLoginResponseDto;
import mannabom_server.manabom.application.auth.dto.response.RefreshTokenResponseDto;
import mannabom_server.manabom.application.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 인증 관련 API 컨트롤러
 *
 * 주요 기능:
 * 1. 카카오 로그인 (인증 코드 → JWT 토큰)
 * 2. 토큰 갱신 (refresh token → 새 토큰)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 로그인
     *
     * 처리 흐름:
     * 1. React Native에서 카카오 OAuth 완료 후 인증 코드 전송
     * 2. AuthService에서 카카오 API 연동 및 사용자 처리
     * 3. 상황별 응답 반환 (기존 유저/신규 유저/연령 제한)
     *
     * @param request 카카오 인증 코드 및 리다이렉트 URI
     * @return 로그인 결과 (토큰 또는 회원가입 안내)
     */
    @PostMapping("/login/kakao")
    public ResponseEntity<KakaoLoginResponseDto> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequestDto request) {

        log.info("카카오 로그인 API 호출");
        log.debug("요청 정보 - redirectUri: {}", request.getRedirectUri());

        KakaoLoginResponseDto response = authService.loginWithKakao(request);

        log.info("카카오 로그인 API 응답 - 상태: {}, 성공: {}",
                response.getUserStatus(), response.isSuccess());

        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신
     *
     * 처리 흐름:
     * 1. 클라이언트에서 refresh token 전송
     * 2. AuthService에서 토큰 유효성 검증
     * 3. 새로운 access/refresh token 발급
     *
     * @param request refresh token
     * @return 새로운 토큰들
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDto> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {

        log.info("토큰 갱신 API 호출");

        RefreshTokenResponseDto response = authService.refreshToken(request);

        log.info("토큰 갱신 API 완료 - 새 토큰 발급 성공");

        return ResponseEntity.ok(response);
    }

}
