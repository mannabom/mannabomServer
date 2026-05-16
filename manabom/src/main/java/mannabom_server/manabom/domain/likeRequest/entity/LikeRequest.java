package mannabom_server.manabom.domain.likeRequest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "like_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_like_from_to", columnNames = {"from_user_id", "to_user_id"})
        },
        indexes = {
                @Index(name = "idx_like_to_status", columnList = "to_user_id,status"),
                @Index(name = "idx_like_from_status", columnList = "from_user_id,status")
        }
)
@Getter
@NoArgsConstructor
public class LikeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private LikeSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LikeStatus status;

    @Column
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public LikeRequest(Long fromUserId, Long toUserId, LikeSource source) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.source = source;
        this.status = LikeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void accept(){
        if (this.status != LikeStatus.PENDING) {
            throw new IllegalStateException("이미 응답한 좋아요 요청입니다.");
        }
        this.status = LikeStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String reason){
        if (this.status != LikeStatus.PENDING) {
            throw new IllegalStateException("이미 응답한 좋아요 요청입니다.");
        }
        this.status = LikeStatus.REJECTED;
        this.rejectReason = reason;
        this.respondedAt = LocalDateTime.now();
    }
}
