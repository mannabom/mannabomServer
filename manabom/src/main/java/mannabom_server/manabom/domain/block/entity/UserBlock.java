package mannabom_server.manabom.domain.block.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.block.enums.UserBlockSource;

import java.time.Instant;

/**
 * blockerUserId 가 blockedUserId 를 차단했다. 적용은 양방향이다.
 */
@Getter
@Entity
@Table(name = "user_block")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_block_id")
    private Long userBlockId;

    @Column(name = "blocker_user_id", nullable = false, updatable = false)
    private Long blockerUserId;

    @Column(name = "blocked_user_id", nullable = false, updatable = false)
    private Long blockedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 20)
    private UserBlockSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserBlock(Long blockerUserId, Long blockedUserId, UserBlockSource source) {
        if (blockerUserId == null || blockedUserId == null) {
            throw new IllegalArgumentException("차단하는 회원과 차단할 회원은 필수입니다.");
        }
        if (blockerUserId.equals(blockedUserId)) {
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
        this.source = source == null ? UserBlockSource.DIRECT : source;
        this.createdAt = Instant.now();
    }
}
