package mannabom_server.manabom.application.gifticon.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGifticonDeliveryFailedEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void notifySender(ChatGifticonDeliveryFailedEvent event) {
        try {
            notificationService.sendNotification(
                    event.senderUserId(),
                    SseEventName.GIFTICON_DELIVERY_FAILED,
                    "기프티콘 발송 실패",
                    "기프티콘을 보내지 못해 결제 환불을 요청했습니다.",
                    Map.of(
                            "paymentId", event.paymentId(),
                            "roomId", event.roomId(),
                            "paymentStatus", event.paymentStatus().name()
                    )
            );
        } catch (RuntimeException exception) {
            log.error(
                    "채팅 기프티콘 발송 실패 알림 전송 실패. senderUserId={}, paymentId={}",
                    event.senderUserId(),
                    event.paymentId(),
                    exception
            );
        }
    }
}
