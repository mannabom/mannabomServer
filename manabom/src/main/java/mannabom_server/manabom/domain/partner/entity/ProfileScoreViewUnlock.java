package mannabom_server.manabom.domain.partner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

@Entity
@Table(
        name = "profile_score_view_unlock",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_score_view_requester_target",
                        columnNames = {"requester_user_id", "target_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileScoreViewUnlock extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unlock_id")
    private Long unlockId;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    public ProfileScoreViewUnlock(Long requesterUserId, Long targetUserId) {
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
    }
}
