package mannabom_server.manabom.domain.deviceToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);
    List<DeviceToken> findAllByUserIdAndActiveTrue(Long userId);
    List<DeviceToken> findAllByActiveTrue();
    List<DeviceToken> findByUserId(Long userId);
}