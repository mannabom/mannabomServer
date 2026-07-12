package mannabom_server.manabom.infrastructure.redis.meetingmatching;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisMatchQueue {
    private final StringRedisTemplate redis;
    private final static String ACTIVE_KEY= "match:active_keys";

    public void add(MeetingMatchingEvent meta){
        String id = String.valueOf(meta.getMeetingId());
        String tKey = timeKey(meta.getSidoCode(), meta.getMemberCount(), meta.getGender());
        redis.execute(new SessionCallback<Object>() {
            @Override
            public List<Object> execute(@NonNull RedisOperations operations) throws DataAccessException {
                RedisOperations<String, String> stringOps = (RedisOperations<String, String>) operations;
                stringOps.multi();

                stringOps.opsForZSet().add(tKey,id, meta.getMatchingStartAtMs());
                stringOps.opsForZSet().add(ageSigKey(meta.getSidoCode(), meta.getSigunguCode(), meta.getMemberCount(), meta.getGender()), id, meta.getAvgage());
                stringOps.opsForZSet().add(ageSidoKey(meta.getSidoCode(), meta.getMemberCount(), meta.getGender()),id, meta.getAvgage());
                stringOps.opsForZSet().add(timeSigKey(meta.getSidoCode(),meta.getSigunguCode(), meta.getMemberCount(), meta.getGender()),id, meta.getMatchingStartAtMs());

                stringOps.opsForSet().add(ACTIVE_KEY,tKey);

                return stringOps.exec();
            }
        });
    }

    public void remove(MeetingMatchingEvent meta){

        String sidoCode = meta.getSidoCode();
        String sigunguCode = meta.getSigunguCode();
        String gender = meta.getGender();
        int memberCount = meta.getMemberCount();

        String id = String.valueOf(meta.getMeetingId());
        String timeKey = timeKey(sidoCode, memberCount, gender);
        String ageSigKey = ageSigKey(sidoCode, sigunguCode, memberCount, gender);
        String ageSidoKey = ageSidoKey(sidoCode,memberCount,gender);
        String timeSigKey = timeSigKey(sidoCode,sigunguCode,memberCount,gender);

        redis.execute(new SessionCallback<Object>() {
            @Override
            public List<Object> execute(@NonNull RedisOperations operations) {
                RedisOperations<String, String> stringOps = (RedisOperations<String, String>) operations;
                stringOps.multi();

                stringOps.opsForZSet().remove(timeKey, id);
                stringOps.opsForZSet().remove(ageSigKey, id);
                stringOps.opsForZSet().remove(ageSidoKey, id);
                stringOps.opsForZSet().remove(timeSigKey,id);

                return stringOps.exec();
            }
        });
    }

    public void removeActiveKey(String queueKey){
        redis.opsForSet().remove(ACTIVE_KEY,queueKey);
    }

    public String timeKey(String sidoCode, int memberCount, String gender){
        return String.format("z:match:time:%s:%d:%s",sidoCode,memberCount,gender);
    }
    public String ageSigKey(String sidoCode, String sigunguCode, int memberCount, String gender){
        return String.format("z:match:ageSig:%s:%s:%d:%s",sidoCode,sigunguCode,memberCount,gender);
    }
    public String ageSidoKey(String sidoCode,int memberCount, String gender){
        return String.format("z:match:ageSido:%s:%d:%s",sidoCode, memberCount, gender);
    }
    public String timeSigKey(String sidoCode,String sigunguCode, int memberCount, String gender){
        return String.format("z:match:timeSig:%s:%s:%d:%s",sidoCode,sigunguCode, memberCount, gender);
    }




    public Set<String> longWaitingCandidates(String sidoCode, int memberCount, String gender, long cutoffTime, int limit){
        return redis.opsForZSet().rangeByScore(
                timeKey(sidoCode,memberCount,gender),
                Double.NEGATIVE_INFINITY,
                cutoffTime,
                0,
                limit
        );
    }
    public Set<String> similarAgeInSigunguCandidates(String sidoCode, String sigunguCode, int membercount, String gender, double age, int limit){
        Set<String> anySigunguCandidate = redis.opsForZSet().rangeByScore(
                ageSigKey(sidoCode,sidoCode+"000",membercount,gender),
                age-3,
                age+3,
                0,
                limit
        );
        Set<String> exactSigunguCandidate = redis.opsForZSet().rangeByScore(
                ageSigKey(sidoCode, sigunguCode,membercount,gender),
                age-3,
                age+3,
                0,
                limit
        );
        return mergeSets(anySigunguCandidate,exactSigunguCandidate);
    }
    public Set<String> anyAgeInSigunguCandidates(String sidoCode, String sigunguCode, int memberCount, String gender, int limit){
        Set<String> anySigunguCandidate = redis.opsForZSet().range(
                timeSigKey(sidoCode, sidoCode+"000", memberCount,gender),
                0,
                limit -1
        );

        Set<String> exactSigunguCandidate = redis.opsForZSet().range(
                timeSigKey(sidoCode, sigunguCode, memberCount,gender),
                0,
                limit -1
        );
       return mergeSets(anySigunguCandidate,exactSigunguCandidate);

    }


    public Set<String> similarAgeInSidoCandidates(String sidoCode, int memberCount, String gender,double age, int limit){
        return redis.opsForZSet().rangeByScore(
                ageSidoKey(sidoCode,memberCount,gender),
                age-3,
                age+3,
                0,
                limit
        );
    }
    public Set<String> anyAgeInSidoCandidates(String sidoCode, int membercount, String gender, int limit)
    {
        return redis.opsForZSet().range(
                timeKey(sidoCode,membercount,gender),
                0,
                limit -1
        );
    }

    public Set<String> longWaitingInSigunguCandidates(String sidoCode, String sigunguCode, int memberCount, String gender, long cutoffTime,int limit){

        Set<String> anySigunguCandidate = redis.opsForZSet().rangeByScore(
                timeSigKey(sidoCode,sidoCode+"000",memberCount,gender),
                Double.NEGATIVE_INFINITY,
                cutoffTime,
                0,
                limit
        );

        Set<String> exactSigunguCandidate = redis.opsForZSet().rangeByScore(
                timeSigKey(sidoCode,sigunguCode, memberCount,gender),
                Double.NEGATIVE_INFINITY,
                cutoffTime,
                0,
                limit
        );
        return mergeSets(anySigunguCandidate,exactSigunguCandidate);
    }

    private Set<String> mergeSets(Set<String> specific, Set<String> any) {
        if (specific == null) specific = new HashSet<>();
        if (any != null) specific.addAll(any);
        return specific;
    }
}
