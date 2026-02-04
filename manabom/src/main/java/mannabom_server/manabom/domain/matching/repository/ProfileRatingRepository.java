package mannabom_server.manabom.domain.matching.repository;

import mannabom_server.manabom.domain.matching.entity.ProfileRating;
import mannabom_server.manabom.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRatingRepository extends JpaRepository<ProfileRating, Long> {

    Optional<ProfileRating> findByFromUserIdAndTargetUserId(Long fromUserId, Long targetUserId);

    boolean existsByFromUserIdAndTargetUserId(Long fromUserId, Long targetUserId);

    int countByTargetUserId(Long targetUserId);
}
