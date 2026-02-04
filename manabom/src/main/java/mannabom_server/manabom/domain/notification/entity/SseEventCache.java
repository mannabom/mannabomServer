package mannabom_server.manabom.domain.notification.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Builder
@Getter
@RedisHash(value = "sse_event",timeToLive = 60*60)
public class SseEventCache {
    @Id
    private String id;

    @Indexed
    private Long userId;
    private String eventName;

    private String data;
}
