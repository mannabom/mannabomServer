package mannabom_server.manabom.application.gifticon.dto.response;

public record GifticonPaymentPrepareResponse(
        Long gifticonPaymentId,
        String orderId,
        String customerKey,
        String orderName,
        int amount,
        String clientKey
) {
}
