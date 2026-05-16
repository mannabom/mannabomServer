package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AdminResetPasswordRequest {
    @NotBlank
    @Size(min = 8)
    private String password;
}
