package mannabom_server.manabom.domain.admin.repository;

import mannabom_server.manabom.domain.admin.entity.AdminAccount;
import mannabom_server.manabom.domain.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {
    Optional<AdminRefreshToken> findByRefreshToken(String refreshToken);

    void deleteByAdminAccount(AdminAccount adminAccount);

    void deleteByRefreshToken(String refreshToken);
}
