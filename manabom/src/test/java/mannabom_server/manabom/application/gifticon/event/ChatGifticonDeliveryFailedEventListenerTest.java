package mannabom_server.manabom.application.gifticon.event;

import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatGifticonDeliveryFailedEventListenerTest {

    @Test
    void notifiesOnlySenderWhenDeliveryNeverReachedRequested() {
        NotificationService notificationService = mock(NotificationService.class);
        ChatGifticonDeliveryFailedEventListener listener =
                new ChatGifticonDeliveryFailedEventListener(notificationService);

        listener.notifySender(new ChatGifticonDeliveryFailedEvent(
                1L,
                20L,
                30L,
                GifticonPaymentStatus.REFUNDED
        ));

        verify(notificationService).sendNotification(
                eq(1L),
                eq(SseEventName.GIFTICON_DELIVERY_FAILED),
                eq("기프티콘 발송 실패"),
                eq("기프티콘을 보내지 못해 결제 환불을 요청했습니다."),
                any()
        );
    }
}
