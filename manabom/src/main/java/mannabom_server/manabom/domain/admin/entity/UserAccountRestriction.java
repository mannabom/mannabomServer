package mannabom_server.manabom.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_account_restrictions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccountRestriction extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserAccountStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @Builder
    public UserAccountRestriction(Long userId,
                                  UserAccountStatus status,
                                  String reason,
                                  LocalDateTime suspendedUntil,
                                  Long updatedByAdminId) {
        this.userId = userId;
        this.status = status;
        this.reason = reason;
        this.suspendedUntil = status == UserAccountStatus.SUSPENDED ? suspendedUntil : null;
        this.updatedByAdminId = updatedByAdminId;
    }

    public void update(UserAccountStatus status, String reason, LocalDateTime suspendedUntil, Long adminId) {
        this.status = status;
        this.reason = reason;
        this.suspendedUntil = status == UserAccountStatus.SUSPENDED ? suspendedUntil : null;
        this.updatedByAdminId = adminId;
    }

    public boolean isAccessBlocked(LocalDateTime now) {
        if (status == UserAccountStatus.WITHDRAWN) {
            return true;
        }
        if (status != UserAccountStatus.SUSPENDED) {
            return false;
        }
        return suspendedUntil == null || suspendedUntil.isAfter(now);
    }

    public UserAccountStatus effectiveStatus(LocalDateTime now) {
        return effectiveStatus(status, suspendedUntil, now);
    }

    public static UserAccountStatus effectiveStatus(UserAccountStatus status, LocalDateTime suspendedUntil, LocalDateTime now) {
        if (status == null) {
            return UserAccountStatus.ACTIVE;
        }
        if (status == UserAccountStatus.SUSPENDED
                && suspendedUntil != null
                && !suspendedUntil.isAfter(now)) {
            return UserAccountStatus.ACTIVE;
        }
        return status;
    }
}
