package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;

import java.time.Instant;

@Getter
@Builder
public class AdminAuditLogResponse {
    private final Long auditId;
    private final Long adminId;
    private final String adminLoginId;
    private final String adminName;
    private final AdminAuditActionType actionType;
    private final AdminAuditTargetType targetType;
    private final Long targetId;
    private final String beforeValue;
    private final String afterValue;
    private final String reason;
    private final String ipAddress;
    private final Instant createdAt;
}
