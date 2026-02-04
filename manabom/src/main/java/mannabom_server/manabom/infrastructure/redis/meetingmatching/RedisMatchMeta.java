package mannabom_server.manabom.infrastructure.redis.meetingmatching;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class RedisMatchMeta {
    private final StringRedisTemplate redis;

    public void saveMeta(MeetingMatchingEvent event){
        String key = metaKey(event.getMeetingId());

        Map<String,String> m = new HashMap<>();
        m.put("meetingId", String.valueOf(event.getMeetingId()));
        m.put("gender",event.getGender());
        m.put("memberCount",String.valueOf(event.getMemberCount()));
        m.put("sidoCode",event.getSidoCode());
        m.put("sigunguCode",event.getSigunguCode());
        m.put("avgAge",String.valueOf(event.getAvgage()));
        m.put("startAt",String.valueOf(event.getMatchingStartAtMs()));

        redis.opsForHash().putAll(key,m);
    }

    public String metaKey(Long meetingId){
        return "match:meta:"+meetingId;
    }

    public MeetingMatchingEvent get(Long meetingId){
        Map<Object,Object> m = redis.opsForHash().entries(metaKey(meetingId));
        if (m == null || m.isEmpty())
            return null;
        return MeetingMatchingEvent.builder()
                .meetingId(meetingId)
                .sidoCode(m.get("sidoCode").toString())
                .gender(m.get("gender").toString())
                .memberCount(Integer.parseInt(m.get("memberCount").toString()))
                .sigunguCode(m.get("sigunguCode").toString())
                .avgage(Double.parseDouble(m.get("avgAge").toString()))
                .matchingStartAtMs(Long.parseLong(m.get("startAt").toString()))
                .build();
    }

    public void delete(Long meetingId){
        redis.delete(metaKey(meetingId));
    }
}
