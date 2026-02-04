package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
}
