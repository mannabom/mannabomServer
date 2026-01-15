package mannabom_server.manabom.domain.deviceToken;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "device_token",
        indexes = {
                @Index(name = "idx_device_token_user", columnList = "userId"),
                @Index(name = "idx_device_token_active", columnList = "active"),
                @Index(name = "idx_device_token_token", columnList = "token", unique = true)
        })
public class DeviceToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    private boolean active = true;

    private Instant lastSeenAt = Instant.now();

    @Builder
    public DeviceToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public void touch(Long currentUserId) {
        this.lastSeenAt = Instant.now();
        this.active = true;
        if(!Objects.equals(currentUserId, userId)){
            userId = currentUserId;
        }
    }

    public void deactivate() {
        this.active = false;
    }
}

