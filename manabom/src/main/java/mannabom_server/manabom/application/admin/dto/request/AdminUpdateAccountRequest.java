package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.AdminAccountStatus;
import mannabom_server.manabom.domain.admin.enums.AdminRole;

import java.util.Set;

@Getter
public class AdminUpdateAccountRequest {
    @NotBlank
    private String name;

    @NotNull
    private Set<AdminRole> roles;

    @NotNull
    private AdminAccountStatus status;
}
