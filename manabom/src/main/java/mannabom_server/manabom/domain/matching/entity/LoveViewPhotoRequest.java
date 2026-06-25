package mannabom_server.manabom.domain.matching.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.matching.enums.PhotoRequestStatus;
import mannabom_server.manabom.domain.user.entity.User;

@Entity
@Table(name = "love_view_photo_requests")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoveViewPhotoRequest extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id")
    private LoveViewRecommendHistory history;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    private PhotoRequestStatus status;

    public void accept(){
        this.status = PhotoRequestStatus.ACCEPTED;
    }
    public void reject(){
        this.status = PhotoRequestStatus.REJECTED;
    }

}
