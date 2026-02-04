package mannabom_server.manabom.application.meeting.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchAtomicOps;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MatchingEventListener {
    private final MeetingMatchingService meetingMatchingService;
    private final RedisMatchAtomicOps atomic;

    @Async
    @EventListener
    public void handleMatchRequest(MeetingMatchingEvent event){
        if(!atomic.tryLock(event.getMeetingId())) return;
        try {
            log.info("Async Matching Start for Team: {}", event.getMeetingId());
            List<Long> opponents = meetingMatchingService.findOpponent(event);

            if(opponents.isEmpty()){
                meetingMatchingService.addToQueue(event);
                return;
            }
            for(Long opponentId: opponents){
                if(atomic.tryLock(opponentId)){
                    try{
                        MeetingMatchingEvent opponent = meetingMatchingService.getMatchingMetaInfo(opponentId);
                        if(opponent== null)
                            continue;

                        boolean removed = meetingMatchingService.confirmAndRemoveOpponent(opponentId,event, opponent.getSigunguCode());
                        if(!removed)
                            continue;
                        try {
                            meetingMatchingService.processMatchSuccess(event, opponent);

                        } catch (Exception e) {
                            meetingMatchingService.rollbackMatch(event, opponent);
                            log.error("매칭 처리 중 오류 발생으로 롤백합니다. My: {}, Opponent: {}", event.getMeetingId(), opponentId, e);
                            throw e;
                        }
                        return;
                    } finally {
                        atomic.tryUnlock(opponentId);
                    }
                }
                else {
                    log.debug("Opponent {}의 락이 이미 걸려있습니다. 다음 시도 ", opponentId);
                }
            }
            log.info("모든 후보가 실패하였으므로 대기열에 등록합니다. {}", event.getMeetingId());
            meetingMatchingService.addToQueue(event);


        }
        catch (Exception e) {
            log.error("Meeting Matching error",e);
            meetingMatchingService.addToQueue(event);
        } finally {
            atomic.tryUnlock(event.getMeetingId());
        }
    }
}
