package mannabom_server.manabom.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AdminAuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AdminAuditTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Builder
    public AdminAuditLog(Long adminId,
                         AdminAuditActionType actionType,
                         AdminAuditTargetType targetType,
                         Long targetId,
                         String beforeValue,
                         String afterValue,
                         String reason,
                         String ipAddress) {
        this.adminId = adminId;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
        this.ipAddress = ipAddress;
    }
}
