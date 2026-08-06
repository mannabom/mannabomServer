package mannabom_server.manabom.application.gifticon.dto.response;

import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;

public record GifticonPaymentResponse(
        Long gifticonPaymentId,
        String orderId,
        int amount,
        GifticonPaymentStatus status,
        GifticonMessageCreationStatus messageCreationStatus,
        Long messageRequestId
) {
    public static GifticonPaymentResponse from(GifticonPayment payment) {
        return new GifticonPaymentResponse(
                payment.getGifticonPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMessageCreationStatus(),
                payment.getMessageRequest() == null
                        ? null
                        : payment.getMessageRequest().getId()
        );
    }
}
