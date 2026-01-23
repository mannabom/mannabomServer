package mannabom_server.manabom.domain.matching.profileMatching.repository;

import mannabom_server.manabom.domain.matching.profileMatching.entity.ProfileRecommendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ProfileRecommendHistoryRepository extends JpaRepository<ProfileRecommendHistory, Long> {

    boolean existsByRequesterUserIdAndTargetUserIdAndRecommendedAtAfter(
            Long requesterUserId, Long targetUserId, LocalDateTime after
    );

    long countByRequesterUserIdAndRecommendedAtBetween(
            Long requesterUserId, LocalDateTime start, LocalDateTime end
    );
}
