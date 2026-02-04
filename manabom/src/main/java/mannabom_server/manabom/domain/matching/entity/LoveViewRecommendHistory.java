package mannabom_server.manabom.domain.matching.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.matching.enums.RecommendType;

import java.time.LocalDateTime;

@Entity
@Table(name = "love_view_recommend_history",
        indexes = {
                @Index(name = "idx_req_time", columnList = "requester_user_id,recommended_at"),
                @Index(name = "idx_req_target_time", columnList = "requester_user_id,target_user_id,recommended_at")
        })
@Getter
@NoArgsConstructor
public class LoveViewRecommendHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommend_type", nullable = false)
    private RecommendType recommendType;

    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    public LoveViewRecommendHistory(Long requesterUserId, Long targetUserId, RecommendType recommendType, LocalDateTime recommendedAt) {
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
        this.recommendType = recommendType;
        this.recommendedAt = recommendedAt;
    }
}