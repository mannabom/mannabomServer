package mannabom_server.manabom.application.pushService.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.deviceToken.DeviceToken;
import mannabom_server.manabom.domain.deviceToken.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    @Transactional
    public void upsert(Long userId, String token) {
        repo.findByToken(token).ifPresentOrElse(
                existing -> existing.touch(userId),
                () -> repo.save(DeviceToken.builder().userId(userId).token(token).build())
        );
    }
}

