package mannabom_server.manabom.application.chat.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.service.SystemMessageService;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import mannabom_server.manabom.application.notification.dto.MatchSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSystemMessageEventHandler {

    private final SystemMessageService systemMessageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchFound(MatchFoundEvent event) {
        try {
            systemMessageService.dispatch(
                    systemMessageService.recordMatchFound(event.matchId(), event.decisionDeadline())
            );
        } catch (RuntimeException e) {
            log.error("매칭 발견 시스템 메시지 처리 실패: matchId={}", event.matchId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchFailure(MatchFailureEvent event) {
        try {
            systemMessageService.dispatch(systemMessageService.recordMatchFailure(
                    event.matchId(),
                    event.triggerUserId(),
                    Boolean.TRUE.equals(event.isByTimeout())
            ));
        } catch (RuntimeException e) {
            log.error("매칭 실패 시스템 메시지 처리 실패: matchId={}", event.matchId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchSuccess(MatchSuccessEvent event) {
        try {
            systemMessageService.dispatch(systemMessageService.recordMatchSuccess(
                    event.matchId(),
                    event.triggerUserId(),
                    event.chatRoomId()
            ));
        } catch (RuntimeException e) {
            log.error("매칭 성사 시스템 메시지 처리 실패: matchId={}", event.matchId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomSystemMessage(ChatSystemMessageEvent event) {
        try {
            systemMessageService.dispatch(systemMessageService.recordForRoom(event));
        } catch (RuntimeException e) {
            log.error("채팅방 시스템 메시지 처리 실패: roomId={}, type={}",
                    event.roomId(), event.type(), e);
        }
    }
}
