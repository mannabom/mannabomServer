package mannabom_server.manabom.infrastructure.redis.meetingmatching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedissonMatchTimeoutConsumer implements ApplicationRunner {

    private final RedissonClient redissonClient;
    private final MeetingMatchingService meetingMatchingService;
    private static final String QUEUE_NAME = "meeting-matching-expiration-queue";


    @Override
    public void run(ApplicationArguments args) {
        new Thread(()->{
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
            log.info("매칭 만료 감시 리스너 시작 (Queue:{})",QUEUE_NAME);

            while(true){
                try{
                    Long matchId = blockingQueue.take();
                    log.info("타임 아웃 이벤트 발생 matchId = {}",matchId);
                    meetingMatchingService.processAutoDecision(matchId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("응답 만료: 매칭 자동 처리 중 에러 발생");
                }
            }
        }).start();
    }
}
