package mannabom_server.manabom.application.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.domain.notification.entity.Notification;
import mannabom_server.manabom.domain.notification.enums.NotificationType;
import mannabom_server.manabom.domain.notification.repository.NotificationRepository;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private PushService pushService;
    @Mock private NotificationRepository notificationRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private NotificationService notificationService;

    @Test
    void storesNotificationAndAlwaysRequestsPush() {
        notificationService.sendNotification(
                2L,
                NotificationType.SYSTEM_MESSAGE,
                "알림 제목",
                "알림 내용",
                Map.of("roomId", 77L)
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.SYSTEM_MESSAGE);

        ArgumentCaptor<PushMessage> pushCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushService).sendToUser(org.mockito.ArgumentMatchers.eq(2L), pushCaptor.capture());
        assertThat(pushCaptor.getValue().data())
                .containsEntry("type", "SYSTEM_MESSAGE")
                .containsEntry("roomId", "77");
    }

    @Test
    void requestsPushOnlyAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            notificationService.sendNotification(
                    2L,
                    NotificationType.SYSTEM_MESSAGE,
                    "알림 제목",
                    "알림 내용",
                    Map.of("roomId", 77L)
            );

            verify(notificationRepository).save(any(Notification.class));
            verifyNoInteractions(pushService);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(pushService).sendToUser(eq(2L), any(PushMessage.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotPropagatePushFailure() {
        doThrow(new RuntimeException("FCM unavailable"))
                .when(pushService)
                .sendToUser(eq(2L), any(PushMessage.class));

        assertThatCode(() -> notificationService.sendNotification(
                2L,
                NotificationType.SYSTEM_MESSAGE,
                "알림 제목",
                "알림 내용",
                Map.of("roomId", 77L)
        )).doesNotThrowAnyException();

        verify(notificationRepository).save(any(Notification.class));
    }
}
