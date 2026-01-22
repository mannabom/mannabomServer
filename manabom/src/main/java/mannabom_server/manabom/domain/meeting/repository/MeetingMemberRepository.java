package mannabom_server.manabom.domain.meeting.repository;

import io.lettuce.core.dynamic.annotation.Param;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember,Long> {
    @Query("SELECT mm FROM MeetingMember mm " +
            "WHERE mm.user.userId = :userId " +
            "AND mm.status = :status")
    Optional<MeetingMember> findByUser_UserIdAndStatus(Long userId, ChatUserStatus status);


    boolean existsByMeeting_IdAndUser_UserIdAndStatus(Long meetingId, Long userId, ChatUserStatus status);


}
