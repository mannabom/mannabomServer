package mannabom_server.manabom.infrastructure.redis.meetingmatching;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisMatchAtomicOps {
    private final StringRedisTemplate redis;
    private final RedissonClient redissonClient;

    private static final long LOCK_TTL_SECONDS = 3;

    // 동시성 문제 해결 -> 여러 큐를 순회하면서 특정 미팅팀 제거
    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>(
            "local id = ARGV[1]; local rm = 0; " +
                    "for i = 1, #KEYS do rm = rm + redis.call('ZREM', KEYS[i], id) end; " +
                    "if rm > 0 then return 1 else return 0 end",
            Long.class
    );

    public boolean tryLock(Long meetingId) {
        RLock lock = redissonClient.getLock(lockKey(meetingId));
        try {
            return lock.tryLock(0, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void tryUnlock(Long meetingId) {
        RLock lock = redissonClient.getLock(lockKey(meetingId));
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public boolean atomicRemove(Long id, List<String> keys) {
        Long result = redis.execute(REMOVE_SCRIPT, keys, String.valueOf(id));
        return result == 1L;
    }

    private String lockKey(Long meetingId) {
        return String.format("lock:match:%d", meetingId);
    }
}