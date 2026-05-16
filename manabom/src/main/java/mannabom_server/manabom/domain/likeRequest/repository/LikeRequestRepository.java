package mannabom_server.manabom.domain.likeRequest.repository;

import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRequestRepository extends JpaRepository<LikeRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LikeRequest lr where lr.id = :id")
    Optional<LikeRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<LikeRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    List<LikeRequest> findAllByToUserIdAndStatus(Long toUserId, LikeStatus status);

    List<LikeRequest> findAllByFromUserIdAndStatus(Long fromUserId, LikeStatus status);

    List<LikeRequest> findAllByToUserId(Long toUserId);

    List<LikeRequest> findAllByFromUserId(Long fromUserId);

    @Query(value = """
    SELECT *
    FROM like_request
    WHERE to_user_id = :userId
      AND status = 'PENDING'
      AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<LikeRequest> findPendingSignalsToMeLast7Days(@Param("userId") Long userId);

    @Query(value = """
    SELECT *
    FROM like_request
    WHERE from_user_id = :userId
      AND status IN ('PENDING', 'REJECTED')
      AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<LikeRequest> findSignalsFromMeLast7Days(@Param("userId") Long userId);

}
