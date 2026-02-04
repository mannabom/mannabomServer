package mannabom_server.manabom.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import mannabom_server.manabom.domain.user.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Builder
@Getter
@Entity
@Table(name = "notification")
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String title;
    private String message;
    private boolean isRead;

    private SseEventName type;
    @Column(columnDefinition = "TEXT") // 혹은 @Lob. 내용이 길어질 수 있으므로 TEXT 권장
    private String data;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {

    }
}
