package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    int countChatMessagesByRoom_Id(Long roomId);

    int countChatMessagesByRoom_IdAndCreatedAtAfter(Long roomId, Instant after);
}
