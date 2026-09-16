package mannabom_server.manabom.application.tingPurchase.service;

import mannabom_server.manabom.application.tingPurchase.port.AppleServerNotificationDecoder;
import mannabom_server.manabom.application.tingPurchase.port.AppleServerNotificationDecoder.AppleNotification;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseRefundService.RefundOutcome;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleServerNotificationServiceTest {

    @Mock private AppleServerNotificationDecoder decoder;
    @Mock private TingPurchaseRefundService refundService;
    @InjectMocks private AppleServerNotificationService service;

    @Test
    void refundNotificationTriggersClawbackByTransactionId() {
        Instant revoked = Instant.parse("2026-09-16T12:00:00Z");
        when(decoder.decode("signed")).thenReturn(new AppleNotification(
                "REFUND", null, "uuid", "Production", "tx-1", "tx-1", "com.mannabom.ting.70", revoked, "1"
        ));
        when(refundService.refundByTransactionId(eq(PurchaseStore.APPLE), eq("tx-1"), any(), eq(revoked)))
                .thenReturn(RefundOutcome.REFUNDED);

        service.handle("signed");

        verify(refundService).refundByTransactionId(PurchaseStore.APPLE, "tx-1", "REFUND:1", revoked);
    }

    @Test
    void nonRefundNotificationsAreIgnored() {
        when(decoder.decode("signed")).thenReturn(new AppleNotification(
                "TEST", null, "uuid", "Sandbox", null, null, null, null, null
        ));

        service.handle("signed");

        verify(refundService, never()).refundByTransactionId(any(), any(), any(), any());
        assertThatThrownBy(() -> service.handle(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
