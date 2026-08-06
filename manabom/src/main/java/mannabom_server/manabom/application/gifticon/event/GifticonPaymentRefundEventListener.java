package mannabom_server.manabom.application.gifticon.event;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GifticonPaymentRefundEventListener {

    private final GifticonPaymentService gifticonPaymentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refund(GifticonPaymentRefundRequestedEvent event) {
        gifticonPaymentService.refund(event.gifticonPaymentId());
    }
}
