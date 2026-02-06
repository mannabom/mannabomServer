package mannabom_server.manabom.domain.matching.repository;

import mannabom_server.manabom.domain.matching.entity.ProfileRating;
import mannabom_server.manabom.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRatingRepository extends JpaRepository<ProfileRating, Long> {

    Optional<ProfileRating> findByFromUserIdAndTargetUserId(Long fromUserId, Long targetUserId);

    boolean existsByFromUserIdAndTargetUserId(Long fromUserId, Long targetUserId);

    int countByTargetUserId(Long targetUserId);

    List<ProfileRating> findAllByTargetUserIdAndScoreGreaterThanEqual(Long targetUserId, int highScoreThreshold);

    @Query(value = """
        SELECT *
        FROM profile_rating
        WHERE target_user_id = :userId
          AND score >= :threshold
          AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<ProfileRating> findHighScoresToMeLast7Days(
            @Param("userId") Long userId,
            @Param("threshold") int threshold
    );

    @Query(value = """
    SELECT *
    FROM profile_rating
    WHERE from_user_id = :userId
      AND score >= :threshold
      AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<ProfileRating> findHighScoresFromMeLast7Days(
            @Param("userId") Long userId,
            @Param("threshold") int threshold
    );
}
