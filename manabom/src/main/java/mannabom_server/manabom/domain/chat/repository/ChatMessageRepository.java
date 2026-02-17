package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

    List<ChatMessage> findByRoomIdAndIdBetween(Long roomId, Long startId, Long endId);

    @Query(
            "SELECT COUNT(m) from ChatMessage m "+
                    "where m.room.id = : roomId " +
                    "and (:lastReadId is null or m.id > :lastReadId)"
    )
    int countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadId") Long lastReadId);



    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.user u JOIN Profile p on p.user.userId = u.userId WHERE m.room.id = :roomId AND m.id > :lastMessageId ORDER BY m.id desc ")
    List<ChatMessage> findChatMessagesAfter(@Param("roomId") Long roomId, @Param("lastReadId") Long lastReadId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.user u JOIN Profile p on p.user.userId = u.userId WHERE m.room.id = :roomId AND m.id > :firstMessageId ORDER BY m.id desc ")
    List<ChatMessage> findChatMessagesBefore(@Param("roomId") Long roomId, @Param("firstMessageId") Long firstMessageId, Pageable pageable);


    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId ORDER BY m.id DESC")
    List<ChatMessage> findLatestMessages(@Param("roomId") Long roomId, Pageable pageable);
}
