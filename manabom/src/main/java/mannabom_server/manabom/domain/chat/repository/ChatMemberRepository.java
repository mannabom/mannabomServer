package mannabom_server.manabom.domain.chat.repository;

import mannabom_server.manabom.domain.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
}
