package mannabom_server.manabom.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.AdminAccountStatus;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "admin_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "admin_account_roles",
            joinColumns = @JoinColumn(name = "admin_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<AdminRole> roles = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdminAccountStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public AdminAccount(String loginId, String passwordHash, String name, Set<AdminRole> roles) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.roles = normalizeRoles(roles);
        this.status = AdminAccountStatus.ACTIVE;
    }

    public void recordLogin(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public void update(String name, Set<AdminRole> roles, AdminAccountStatus status) {
        this.name = name;
        this.roles = normalizeRoles(roles);
        this.status = status;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return status == AdminAccountStatus.ACTIVE;
    }

    public Set<AdminRole> getRoles() {
        return normalizeRoles(roles);
    }

    private Set<AdminRole> normalizeRoles(Set<AdminRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return new LinkedHashSet<>(Set.of(AdminRole.SUPPORT));
        }
        return new LinkedHashSet<>(roles);
    }
}
