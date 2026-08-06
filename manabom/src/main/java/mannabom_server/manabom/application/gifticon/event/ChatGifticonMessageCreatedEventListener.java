package mannabom_server.manabom.application.gifticon.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGifticonMessageCreatedEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyReceiver(ChatGifticonMessageCreatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomId(),
                ChatMessageEvent.builder()
                        .roomId(event.roomId())
                        .senderUserId(event.senderUserId())
                        .messageType(ChatMessageType.GIFTICON.name())
                        .content(event.content())
                        .messageId(event.messageId())
                        .sendAt(event.createdAt())
                        .gifticon(event.gifticon())
                        .build()
        );

        try {
            notificationService.sendNotification(
                    event.receiverUserId(),
                    SseEventName.NEW_CHAT_MESSAGE,
                    event.senderNickname(),
                    "🎁 기프티콘을 보냈습니다.",
                    Map.of(
                            "roomId", event.roomId(),
                            "messageId", event.messageId(),
                            "messageType", ChatMessageType.GIFTICON.name()
                    )
            );
        } catch (RuntimeException exception) {
            log.error(
                    "채팅 기프티콘 수신 알림 전송 실패. roomId={}, messageId={}, receiverUserId={}",
                    event.roomId(),
                    event.messageId(),
                    event.receiverUserId(),
                    exception
            );
        }
    }
}
