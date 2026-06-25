package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminUpdateUserStatusRequest {
    @NotNull(message = "status는 필수입니다.")
    private UserAccountStatus status;

    private LocalDateTime suspendedUntil;

    private String reason;
}
