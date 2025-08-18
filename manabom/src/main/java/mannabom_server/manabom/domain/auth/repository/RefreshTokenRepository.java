package mannabom_server.manabom.domain.auth.repository;

import mannabom_server.manabom.domain.auth.entity.RefreshToken;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /**
     * 리프레시 토큰으로 조회
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    /**
     * 사용자로 조회
     */
    Optional<RefreshToken> findByUser(User user);

    /**
     * 사용자 기본 토큰 삭제 (로그아웃 시)
     */
    void deleteByUser(User user);

    /**
     * 만료된 토큰들 삭제 (정리 작업용)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
