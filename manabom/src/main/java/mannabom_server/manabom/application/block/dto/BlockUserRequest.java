package mannabom_server.manabom.application.block.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BlockUserRequest(
        @NotNull(message = "차단할 회원의 프로필 ID는 필수입니다.")
        @Positive(message = "차단할 회원의 프로필 ID는 0보다 커야 합니다.")
        Long targetProfileId
) {
}
