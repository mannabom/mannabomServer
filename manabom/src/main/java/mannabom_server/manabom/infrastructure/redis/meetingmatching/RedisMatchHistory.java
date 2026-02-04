package mannabom_server.manabom.infrastructure.redis.meetingmatching;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisMatchHistory {
    private final StringRedisTemplate redis;
    private static final Duration HISTORY_TTL = Duration.ofDays(30);

    public void addHistory(Long me, Long opponent){
        String myKey = historyKey(me);
        String oppKey = historyKey(opponent);

        redis.opsForSet().add(myKey,String.valueOf(opponent));
        redis.opsForSet().add(oppKey,String.valueOf(me));

        redis.expire(myKey,HISTORY_TTL);
        redis.expire(oppKey,HISTORY_TTL);
    }
    public Set<Long> getHistory(Long me){
        Set<String> members = redis.opsForSet().members(historyKey(me));

        if(members==null || members.isEmpty())
            return Collections.emptySet();

        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }
    public void removeHistory(Long meetingId, Long opponentId){
        redis.opsForSet().remove(historyKey(meetingId),String.valueOf(opponentId));
        redis.opsForSet().remove(historyKey(opponentId),String.valueOf(meetingId));
    }
    private String historyKey(Long meetingId){
        return "history:match:"+meetingId;
    }

}
