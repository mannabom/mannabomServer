package mannabom_server.manabom.domain.partner.repository;

import mannabom_server.manabom.domain.partner.entity.ProfileScoreViewUnlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileScoreViewUnlockRepository extends JpaRepository<ProfileScoreViewUnlock, Long> {
    boolean existsByRequesterUserIdAndTargetUserId(Long requesterUserId, Long targetUserId);
}
