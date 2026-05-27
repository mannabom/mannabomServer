package mannabom_server.manabom.infrastructure.security.admin;

import mannabom_server.manabom.domain.admin.enums.AdminRole;

import java.util.Set;

public record AdminPrincipal(Long adminId, String loginId, Set<AdminRole> roles) {
    public boolean hasRole(AdminRole role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(AdminRole... candidates) {
        for (AdminRole role : candidates) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
