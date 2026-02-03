package mannabom_server.manabom.domain.messageRequest.repository;

import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRequestRepository extends JpaRepository<MessageRequest, Long> {

    Optional<MessageRequest> findTopByFromUserIdAndToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId);

    Optional<MessageRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    long countByToUserIdAndStatus(Long toUserId, MessageRequestStatus status);
}
