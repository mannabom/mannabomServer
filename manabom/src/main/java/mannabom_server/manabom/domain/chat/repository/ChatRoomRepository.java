package mannabom_server.manabom.domain.chat.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom,Long> {

    Optional<ChatRoom> findByMeeting(Meeting meeting);

    Optional<ChatRoom> findByMatch(MeetingMatch match);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cr from ChatRoom cr where cr.id = :chatRoomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") Long chatRoomId);
}
