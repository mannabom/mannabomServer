package mannabom_server.manabom.application.admin.dto.response;

import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;

import java.time.Instant;
import java.util.List;

public record AdminGifticonPaymentResponse(
        Long gifticonPaymentId,
        String orderId,
        String maskedPaymentKey,
        Long userId,
        Long targetProfileId,
        Long gifticonProductId,
        String productName,
        int amount,
        GifticonPaymentStatus paymentStatus,
        String paymentStatusDescription,
        GifticonMessageCreationStatus messageCreationStatus,
        String messageCreationStatusDescription,
        int messageCreationAttemptCount,
        Instant lastMessageCreationAttemptAt,
        String messageCreationFailureReason,
        Long messageRequestId,
        MessageRequestStatus messageRequestStatus,
        int refundAttemptCount,
        Instant lastRefundAttemptAt,
        String paymentFailureReason,
        Instant approvedAt,
        Instant refundedAt,
        Instant createdAt,
        Instant updatedAt,
        List<AttentionReason> attentionReasons,
        String tossPaymentStatus,
        Boolean tossStatusMismatch,
        String tossVerificationError
) {
    public record AttentionReason(
            String code,
            String description
    ) {
    }
}
