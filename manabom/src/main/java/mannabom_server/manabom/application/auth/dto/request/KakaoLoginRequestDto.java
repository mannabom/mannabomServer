package mannabom_server.manabom.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequestDto {

    @NotBlank(message = "accessToken이 비어있습니다")
    private String accessToken;       // 카카오에 대한 accessToken

}
