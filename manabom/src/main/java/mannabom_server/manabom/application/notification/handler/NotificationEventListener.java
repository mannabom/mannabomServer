package mannabom_server.manabom.application.notification.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import mannabom_server.manabom.application.notification.dto.MatchSuccessEvent;
import mannabom_server.manabom.application.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase =  TransactionPhase.AFTER_COMMIT)
    public void handleMatchSuccess(MatchSuccessEvent event){
        notificationService.sendMatchSuccess(event.matchId(), event.chatRoomId(), event.isByTimeout());
    }

    @Async
    @TransactionalEventListener(phase =  TransactionPhase.AFTER_COMMIT)     public void handleMatchFailure(MatchFailureEvent event){
        notificationService.sendMatchFailure(event.matchId(), event.triggerUserId(), event.isByTimeout());
    }

    @Async
    @TransactionalEventListener(phase =  TransactionPhase.AFTER_COMMIT)
    public void handleMatchFound(MatchFoundEvent event){
        notificationService.sendMatchFound(event.matchId(),event.decisionDeadline());
    }

}
