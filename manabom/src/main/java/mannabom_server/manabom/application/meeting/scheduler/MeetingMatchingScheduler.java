package mannabom_server.manabom.application.meeting.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchAtomicOps;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchQueue;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class MeetingMatchingScheduler {
    private static final String ACTIVE_KEYS = "match:active_keys";
    private static final int BATCH_SIZE = 20;
    private final MeetingMatchingService meetingMatchingService;
    private final RedisMatchQueue redisMatchQueue;
    private final StringRedisTemplate redis;
    private final RedisMatchAtomicOps atomic;

    @Scheduled(
            initialDelayString = "${meeting.match.scheduler.initial-delay:1800000}",
            fixedDelayString = "${meeting.match.scheduler.fixed-delay:1800000}"     //1800000ms == 30분
    )
    public void runBatchMatching() {
        log.info("[매칭 스케쥴러] 주기적 매칭 시작");
        int matchCount = 0;

        Set<String> activeQueues = redis.opsForSet().members(ACTIVE_KEYS);
        if (activeQueues == null || activeQueues.isEmpty()) return;

        for (String queue : activeQueues) {
            Set<String> waitingIds = redis.opsForZSet().range(queue, 0, BATCH_SIZE - 1);

            if (waitingIds == null || waitingIds.isEmpty()) {
                redisMatchQueue.removeActiveKey(queue);
                continue;
            }

            for (String waiting : waitingIds) {
                try {
                    if (processSingleBatchMatch(Long.parseLong(waiting))) {
                        matchCount++;
                    }
                } catch (Exception e) {
                    log.error("[매칭 스케쥴러] 유저 {} 처리 중 예상치 못한 오류: {}", waiting, e.getMessage());
                }
            }
        }
        log.info("[매칭 스케쥴러] 주기적 매칭 완료 매칭 완료 개수 : {}", matchCount);
    }

    private boolean processSingleBatchMatch(Long myId) {
        if (!atomic.tryLock(myId)) return false;
        try {
            MeetingMatchingEvent me = meetingMatchingService.getMatchingMetaInfo(myId);
            if (me == null) return false;

            List<Long> opponents = meetingMatchingService.findOpponent(me);
            for (Long opponentId : opponents) {
                if (atomic.tryLock(opponentId)) {
                    try {
                        MeetingMatchingEvent opponent = meetingMatchingService.getMatchingMetaInfo(opponentId);
                        if (opponent == null) continue;

                        if (!meetingMatchingService.confirmAndRemoveOpponent(opponentId, me, opponent.getSigunguCode())) {
                            log.info("후보 {}가 그새 사라졌습니다. 다음 후보를 찾습니다.", opponentId);
                            continue;
                        }
                        try {
                            meetingMatchingService.processMatchSuccess(me, opponent);
                            log.info("[매칭 스케쥴러] 매칭 성공!{}<->{}",myId, opponentId);
                            return true;
                        } catch (Exception e) {
                            meetingMatchingService.rollbackMatch(me, opponent);
                            log.error("[매칭 스케쥴러] DB처리 오류로 롤백:{}<->{}", myId, opponentId);
                            return false;
                        }
                    } finally {
                        atomic.tryUnlock(opponentId);
                    }
                }
            }
        } finally {
            atomic.tryUnlock(myId);
        }
        return false;
    }

}

