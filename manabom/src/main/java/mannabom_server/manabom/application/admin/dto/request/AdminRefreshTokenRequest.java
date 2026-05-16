package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRefreshTokenRequest {
    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
