package mannabom_server.manabom.domain.messageRequest.repository;

import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRequestRepository extends JpaRepository<MessageRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mr from MessageRequest mr where mr.id = :id")
    Optional<MessageRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<MessageRequest> findTopByFromUserIdAndToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId);

    Optional<MessageRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    List<MessageRequest> findAllByToUserIdAndStatus(Long toUserId, MessageRequestStatus status);

    long countByToUserIdAndStatus(Long toUserId, MessageRequestStatus status);

    @Query(value = """
    SELECT *
    FROM message_request
    WHERE to_user_id = :userId
      AND status = 'PENDING'
      AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<MessageRequest> findPendingSignalsToMeLast7Days(@Param("userId") Long userId);

    @Query(value = """
    SELECT *
    FROM message_request
    WHERE from_user_id = :userId
      AND status IN ('PENDING', 'REJECTED')
      AND created_at >= (now() AT TIME ZONE 'Asia/Seoul') - interval '7 days'
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<MessageRequest> findSignalsFromMeLast7Days(@Param("userId") Long userId);
}
