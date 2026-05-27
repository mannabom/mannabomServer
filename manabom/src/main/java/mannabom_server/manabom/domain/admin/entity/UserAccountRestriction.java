package mannabom_server.manabom.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

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

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @Builder
    public UserAccountRestriction(Long userId, UserAccountStatus status, String reason, Long updatedByAdminId) {
        this.userId = userId;
        this.status = status;
        this.reason = reason;
        this.updatedByAdminId = updatedByAdminId;
    }

    public void update(UserAccountStatus status, String reason, Long adminId) {
        this.status = status;
        this.reason = reason;
        this.updatedByAdminId = adminId;
    }
}
