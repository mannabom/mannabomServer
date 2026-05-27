package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.AdminAccountStatus;
import mannabom_server.manabom.domain.admin.enums.AdminRole;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class AdminAccountResponse {
    private final Long adminId;
    private final String loginId;
    private final String name;
    private final Set<AdminRole> roles;
    private final AdminAccountStatus status;
    private final LocalDateTime lastLoginAt;
    private final Instant createdAt;
}
