package mannabom_server.manabom.application.gifticon.dto.response;

import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;

public record GifticonPaymentResponse(
        Long gifticonPaymentId,
        String orderId,
        int amount,
        GifticonPaymentStatus status,
        GifticonMessageCreationStatus messageCreationStatus,
        Long messageRequestId,
        GifticonPaymentPurpose purpose,
        Long chatRoomId,
        Long receiverUserId,
        Long chatMessageId,
        GifticonOrderStatus gifticonOrderStatus,
        String gifticonOrderFailureReason
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
                        : payment.getMessageRequest().getId(),
                payment.getPurpose(),
                payment.getChatRoom() == null ? null : payment.getChatRoom().getId(),
                payment.getReceiverUserId(),
                payment.getChatMessage() == null ? null : payment.getChatMessage().getId(),
                payment.getGifticonOrder() == null
                        ? null
                        : payment.getGifticonOrder().getStatus(),
                payment.getGifticonOrder() == null
                        ? null
                        : payment.getGifticonOrder().getFailureReason()
        );
    }
}
