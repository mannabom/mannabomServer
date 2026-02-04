package mannabom_server.manabom.domain.matching.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.matching.enums.RecommendType;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_recommend_history",
        indexes = {
                @Index(name = "idx_req_time", columnList = "requester_user_id,recommended_at"),
                @Index(name = "idx_req_target_time", columnList = "requester_user_id,target_user_id,recommended_at")
        })
@Getter
@NoArgsConstructor
public class ProfileRecommendHistory {

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

    public ProfileRecommendHistory(Long requesterUserId, Long targetUserId, RecommendType type, LocalDateTime recommendedAt) {
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
        this.recommendType = type;
        this.recommendedAt = recommendedAt;
    }
}
