package mannabom_server.manabom.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 요청 Dto
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDto {

    @NotBlank(message = "디바이스 토큰이 필요합니다.")
    private String deviceToken;
}
