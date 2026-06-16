package mannabom_server.manabom.domain.meeting.repository;

import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember,Long> {
    @Query("SELECT mm FROM MeetingMember mm " +
            "WHERE mm.user.userId = :userId " +
            "AND mm.status = :status")
    Optional<MeetingMember> findByUser_UserIdAndStatus(@Param(value = "userId") Long userId,@Param(value = "status") ChatUserStatus status);


    boolean existsByMeeting_IdAndUser_UserIdAndStatus(Long meetingId, Long userId, ChatUserStatus status);

    @Query(
            "Select mm.meeting from MeetingMember mm Join mm.meeting m " +
                    "where mm.user.userId = :userId and mm.meetingRole = :role and mm.status = :status"
    )
    Optional<Meeting> findMeetingByRole(@Param(value = "userId") Long userId, @Param(value = "role") MeetingRole meetingRole, @Param("status") ChatUserStatus status);

    @Query("SELECT mm FROM MeetingMember mm " +
            "WHERE mm.meeting.id = :meetingId " +
            "AND mm.status = :status")
    List<MeetingMember> findByMeetingIdAndStatus(Long meetingId, ChatUserStatus status);

    boolean existsByMeeting_IdAndUser_UserIdAndStatusAndMeetingRole(Long meetingId, Long userId, ChatUserStatus status,MeetingRole role);

    @Query(
            "select mm.user.userId from MeetingMember mm "
                    +"where mm.meeting.id = :meetingId "+
                    "and mm.meetingRole = :role "+
                    "and mm.status = :status "
    )
    Optional<Long> findUserIdByMeetingIdAndRole(@Param(value = "meetingId")Long meetingId, @Param(value = "role") MeetingRole role, @Param(value = "status") ChatUserStatus status);

    @Query(
            "select mm from MeetingMember mm "
                    +"where mm.meeting.id = :meetingId "+
                    "and mm.user.userId = :userId "+
                    "and mm.status = :status "
    )
    Optional<MeetingMember> findByMeeting_IdAndUser_UserIdAndStatus(@Param(value = "meetingId")Long meetingId, @Param(value = "userId")Long userId, @Param(value = "status") ChatUserStatus status);
}
