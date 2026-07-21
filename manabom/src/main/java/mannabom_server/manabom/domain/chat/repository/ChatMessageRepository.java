package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.user u WHERE m.room.id = :roomId AND m.id > :lastReadId ORDER BY m.id ASC")
    List<ChatMessage> findChatMessagesAfter(@Param("roomId") Long roomId, @Param("lastReadId") Long lastReadId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.user u WHERE m.room.id = :roomId AND m.id < :firstMessageId ORDER BY m.id desc ")
    List<ChatMessage> findChatMessagesBefore(@Param("roomId") Long roomId, @Param("firstMessageId") Long firstMessageId, Pageable pageable);


    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId ORDER BY m.id DESC")
    List<ChatMessage> findLatestMessages(@Param("roomId") Long roomId, Pageable pageable);

    boolean existsByIdAndRoomId(Long messageId, Long roomId);

    boolean existsByRoomIdAndIdGreaterThan(Long roomId, Long messageId);

    int countChatMessagesByRoom_IdAndUserIsNotNull(Long roomId);

    int countChatMessagesByRoom_IdAndCreatedAtAfterAndUserIsNotNull(Long roomId, Instant after);
}
