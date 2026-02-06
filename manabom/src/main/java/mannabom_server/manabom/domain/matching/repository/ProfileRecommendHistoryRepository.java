package mannabom_server.manabom.domain.matching.repository;

import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProfileRecommendHistoryRepository extends JpaRepository<ProfileRecommendHistory, Long> {

    boolean existsByRequesterUserIdAndTargetUserIdAndRecommendedAtAfter(
            Long requesterUserId, Long targetUserId, LocalDateTime after
    );

    long countByRequesterUserIdAndRecommendedAtBetween(
            Long requesterUserId, LocalDateTime start, LocalDateTime end
    );

    List<ProfileRecommendHistory> findByRequesterUserIdAndRecommendedAtGreaterThanEqualAndRecommendedAtLessThanOrderByRecommendedAtDesc(
            Long requesterUserId,
            LocalDateTime from,
            LocalDateTime to
    );
}
