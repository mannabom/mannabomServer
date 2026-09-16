package mannabom_server.manabom.domain.meeting.repository;

import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingCancellationVoteRepository
        extends JpaRepository<MeetingCancellationVote, Long> {

    Optional<MeetingCancellationVote> findByRequest_IdAndUser_UserId(
            Long requestId,
            Long userId
    );

    List<MeetingCancellationVote> findAllByRequest_IdOrderById(Long requestId);

    long countByRequest_IdAndDecision(
            Long requestId,
            CancellationVoteDecision decision
    );
}
