package mannabom_server.manabom.domain.messageRequest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;

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
    @Column(name = "source", nullable = false)
    private MessageSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRequestStatus status;

    @Column
    private String rejectReason;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="responded_at")
    private LocalDateTime respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gifticon_product_id")
    private GifticonProduct gifticonProduct;

    public MessageRequest(
            Long fromUserId,
            Long toUserId,
            String message,
            MessageSource source,
            GifticonProduct gifticonProduct
    ) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.message = message;
        this.source = source;
        this.gifticonProduct = gifticonProduct;
        this.status = MessageRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void updateMessage(String message) {
        this.message = message;
    }

    public void accept() {
        if (this.status != MessageRequestStatus.PENDING) {
            throw new IllegalStateException("이미 응답한 메시지 요청입니다.");
        }
        this.status = MessageRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (this.status != MessageRequestStatus.PENDING) {
            throw new IllegalStateException("이미 응답한 메시지 요청입니다.");
        }
        this.status = MessageRequestStatus.REJECTED;
        this.rejectReason = reason;
        this.respondedAt = LocalDateTime.now();
    }
}
