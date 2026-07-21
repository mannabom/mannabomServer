package mannabom_server.manabom.domain.meeting.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeetingCancellationRequestRepository
        extends JpaRepository<MeetingCancellationRequest, Long> {

    boolean existsByMeetingMatch_IdAndStatus(
            Long meetingMatchId,
            MeetingCancellationStatus status
    );

    Optional<MeetingCancellationRequest> findByMeetingMatch_IdAndStatus(
            Long meetingMatchId,
            MeetingCancellationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MeetingCancellationRequest r where r.id = :requestId")
    Optional<MeetingCancellationRequest> findByIdForUpdate(
            @Param("requestId") Long requestId
    );

    List<MeetingCancellationRequest> findAllByStatusAndExpiresAtLessThanEqual(
            MeetingCancellationStatus status,
            Instant now
    );
}
