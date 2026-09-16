package mannabom_server.manabom.application.gifticon.event;

import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;

public record ChatGifticonDeliveryFailedEvent(
        Long senderUserId,
        Long paymentId,
        Long roomId,
        GifticonPaymentStatus paymentStatus
) {
}
