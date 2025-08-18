package mannabom_server.manabom.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String authorizationCode;       // 카카오 OAuth에서 받은 인증 코드

    @NotBlank(message = "리다이렉트 URI는 필수입니다.")
    private String redirectUri;             // 앱의 리다이렉트 URI

    public KakaoLoginRequestDto(String authorizationCode, String redirectUri) {
        this.authorizationCode = authorizationCode;
        this.redirectUri = redirectUri;
    }
}
