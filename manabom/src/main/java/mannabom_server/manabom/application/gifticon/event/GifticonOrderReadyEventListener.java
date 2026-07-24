package mannabom_server.manabom.application.gifticon.event;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.service.GifticonOrderProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GifticonOrderReadyEventListener {

    private final GifticonOrderProcessor gifticonOrderProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestGift(GifticonOrderReadyEvent event) {
        gifticonOrderProcessor.process(event.gifticonOrderId());
    }
}
