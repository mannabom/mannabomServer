package mannabom_server.manabom.domain.partner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

@Entity
@Table(
        name = "profile_extra_photo_unlock",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_unlock_requester_target_photo",
                        columnNames = {"requester_user_id", "target_user_id", "photo_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileExtraPhotoUnlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unlock_id")
    private Long unlockId;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    public ProfileExtraPhotoUnlock(Long requesterUserId, Long targetUserId, Long photoId) {
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
        this.photoId = photoId;
    }
}
