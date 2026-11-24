package mannabom_server.manabom.domain.chat.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom,Long> {

    Optional<ChatRoom> findByMeeting(Meeting meeting);

    Optional<ChatRoom> findByMeeting_Id(Long meetingId);

    Optional<ChatRoom> findByMatch(MeetingMatch match);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cr from ChatRoom cr where cr.id = :chatRoomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") Long chatRoomId);

    /** 두 회원이 함께 속한 지정 유형·상태의 채팅방 (차단 시 1:1 방을 닫는 데 쓴다) */
    @Query("""
            select cr
              from ChatRoom cr
             where cr.type in :types
               and cr.chatStatus = :chatStatus
               and exists (select 1 from ChatMember m1 where m1.room = cr and m1.user.userId = :userId)
               and exists (select 1 from ChatMember m2 where m2.room = cr and m2.user.userId = :otherUserId)
            """)
    List<ChatRoom> findRoomsBetween(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId,
            @Param("types") Collection<ChatRoomType> types,
            @Param("chatStatus") ChatStatus chatStatus
    );
}
