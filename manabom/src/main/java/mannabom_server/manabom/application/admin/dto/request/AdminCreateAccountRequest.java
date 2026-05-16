package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.AdminRole;

import java.util.Set;

@Getter
public class AdminCreateAccountRequest {
    @NotBlank
    private String loginId;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String name;

    @NotNull
    private Set<AdminRole> roles;
}
