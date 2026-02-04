package mannabom_server.manabom.domain.matching.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

@Entity
@Table(
        name = "profile_rating",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_profile_rating_from_to",
                        columnNames = {"from_user_id", "target_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileRating extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_rating_id")
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "score", nullable = false)
    private int score;

    @Builder
    public ProfileRating(Long fromUserId, Long targetUserId, int score) {
        this.fromUserId = fromUserId;
        this.targetUserId = targetUserId;
        this.score = score;
    }

    public void changeScore(int newScore) {
        this.score = newScore;
    }
}
