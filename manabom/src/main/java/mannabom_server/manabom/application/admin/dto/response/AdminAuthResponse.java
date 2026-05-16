package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.AdminRole;

import java.util.Set;

@Getter
@Builder
public class AdminAuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final Long adminId;
    private final String loginId;
    private final String name;
    private final Set<AdminRole> roles;
}
