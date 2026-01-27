package mannabom_server.manabom.domain.matching.loveViewMatching.repository;

import mannabom_server.manabom.domain.matching.loveViewMatching.entity.LoveViewRecommendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LoveViewRecommendHistoryRepository extends JpaRepository<LoveViewRecommendHistory, Long> {

    boolean existsByRequesterUserIdAndTargetUserIdAndRecommendedAtAfter(
            Long requesterUserId, Long targetUserId, LocalDateTime after
    );

    long countByRequesterUserIdAndRecommendedAtBetween(
            Long requesterUserId, LocalDateTime start, LocalDateTime end
    );
}