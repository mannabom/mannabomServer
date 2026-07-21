package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    int countChatMemberByRoomIdAndStatus(Long roomId, ChatMemberStatus status);

    Optional<ChatMember> findByRoomIdAndUser_UserIdAndStatus(Long roomId, Long userId, ChatMemberStatus status);

    boolean existsByRoomIdAndUser_UserIdAndStatus(Long roomId, Long userId, ChatMemberStatus status);

    boolean existsByRoomIdAndUser_UserId(Long roomId, Long userId);

    List<ChatMember> findAllByUser_UserIdAndStatus(Long userId, ChatMemberStatus status);

    List<ChatMember> findAllByUser_UserIdAndStatusAndRoom_ChatStatus(
            Long userId,
            ChatMemberStatus status,
            ChatStatus chatStatus
    );

    List<ChatMember> findByRoomId(Long roomId);

    @Query("SELECT cm, u, p, pi FROM ChatMember cm " +
            "JOIN cm.user u " +
            "JOIN Profile p ON p.user.userId = u.userId " +
            "LEFT JOIN ProfileImage pi ON pi.profile = p AND pi.isMain = true " + // 메인 사진만 조인
            "WHERE cm.room.id = :roomId " +
            "AND cm.status = :status")
    List<Object[]> findAllActiveMembersWithMainImage(
            @Param("roomId") Long roomId,
            @Param("status") ChatMemberStatus status
    );
    List<ChatMember> findAllByRoomIdAndStatus(Long roomId, ChatMemberStatus status);

}
