package mannabom_server.manabom.application.meeting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingTimeoutScheduler;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonMatchTimeoutScheduler implements MeetingMatchingTimeoutScheduler {

    private final RedissonClient redissonClient;
    private static final String QUEUE_NAME = "meeting-matching-expiration-queue";

    @Override
    public void scheduleExpiration(Long matchId) {
        RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
        RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        delayedQueue.offer(matchId,24, TimeUnit.HOURS);

        log.info("매칭 타임아웃 예약 완료: MATCHID={}",matchId);
    }
}
