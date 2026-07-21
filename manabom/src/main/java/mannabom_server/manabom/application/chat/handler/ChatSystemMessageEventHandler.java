package mannabom_server.manabom.application.chat.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.service.SystemMessageService;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSystemMessageEventHandler {

    private final SystemMessageService systemMessageService;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchingStarted(MeetingMatchingEvent event) {
        try {
            ChatMessageEvent message = systemMessageService.recordMatchingStarted(event.getMeetingId());
            systemMessageService.broadcast(message);
        } catch (RuntimeException e) {
            log.error("매칭 시작 시스템 메시지 처리 실패: meetingId={}", event.getMeetingId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchFound(MatchFoundEvent event) {
        try {
            systemMessageService.broadcast(systemMessageService.recordMatchFound(event.matchId()));
        } catch (RuntimeException e) {
            log.error("매칭 발견 시스템 메시지 처리 실패: matchId={}", event.matchId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchFailure(MatchFailureEvent event) {
        try {
            systemMessageService.broadcast(systemMessageService.recordMatchFailure(
                    event.matchId(),
                    Boolean.TRUE.equals(event.isByTimeout())
            ));
        } catch (RuntimeException e) {
            log.error("매칭 실패 시스템 메시지 처리 실패: matchId={}", event.matchId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomSystemMessage(ChatSystemMessageEvent event) {
        try {
            ChatMessageEvent message = systemMessageService.recordForRoom(event.roomId(), event.content());
            systemMessageService.broadcast(message);
        } catch (RuntimeException e) {
            log.error("채팅방 시스템 메시지 처리 실패: roomId={}", event.roomId(), e);
        }
    }
}
