package mannabom_server.manabom.domain.messageRequest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "message_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_msg_from_to", columnNames = {"from_user_id", "to_user_id"})
        },
        indexes = {
                @Index(name = "idx_msgreq_to_status", columnList = "to_user_id,status"),
                @Index(name = "idx_msgreq_from_status", columnList = "from_user_id,status")
        }
)
@Getter
@NoArgsConstructor
public class MessageRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name="to_user_id", nullable = false)
    private Long toUserId;

    @Column(name="message", length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRequestStatus status;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="responded_at")
    private LocalDateTime respondedAt;

    public MessageRequest(Long fromUserId, Long toUserId, String message) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.message = message;
        this.status = MessageRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void updateMessage(String message) {
        this.message = message;
    }

    public void accept() {
        this.status = MessageRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MessageRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
