package mannabom_server.manabom.domain.meeting.repository;

import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingVerificationRepository extends JpaRepository<MeetingVerification, Long> {
    Optional<MeetingVerification> findByRoomId(Long chatRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from MeetingVerification v join fetch v.room where v.id = :id")
    Optional<MeetingVerification> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select v.id
            from MeetingVerification v
            where v.isVerified = false
              and v.failureNotifiedAt is null
              and v.expiresAt is not null
              and v.expiresAt <= :now
            """)
    List<Long> findFailureNotificationTargetIds(@Param("now") Instant now);
}
