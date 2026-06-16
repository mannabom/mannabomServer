package mannabom_server.manabom.domain.meeting.repository;

import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingVerificationRepository extends JpaRepository<MeetingVerification, Long> {
    Optional<MeetingVerification> findByRoomId(Long chatRoomId);
}
