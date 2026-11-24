package mannabom_server.manabom.application.block.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.block.repository.UserBlockRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 추천·요청·프로필 조회에서 차단 관계를 확인한다. 차단은 양방향으로 적용한다.
 * 상대에게 차단 사실을 알리지 않도록 호출하는 쪽은 일반적인 오류 문구를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class UserBlockPolicy {

    private final UserBlockRepository userBlockRepository;

    public boolean isBlockedBetween(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) {
            return false;
        }
        return userBlockRepository.existsBetween(userId, otherUserId);
    }

    /** 내가 차단했거나 나를 차단한 회원 ID */
    public Set<Long> blockedUserIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userBlockRepository.findBlockedUserIdsInEitherDirection(userId);
    }

    public void ensureNotBlocked(Long userId, Long otherUserId, String message) {
        if (isBlockedBetween(userId, otherUserId)) {
            throw new IllegalStateException(message);
        }
    }
}
