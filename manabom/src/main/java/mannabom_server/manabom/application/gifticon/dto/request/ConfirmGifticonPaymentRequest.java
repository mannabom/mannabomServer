package mannabom_server.manabom.application.gifticon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConfirmGifticonPaymentRequest(
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,
        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,
        @Positive(message = "amount는 0보다 커야 합니다.")
        int amount
) {
}
