package mannabom_server.manabom.application.gifticon.dto.request;

import jakarta.validation.constraints.NotNull;

public record PrepareChatGifticonPaymentRequest(
        @NotNull(message = "gifticonProductId는 필수입니다.")
        Long gifticonProductId,
        @NotNull(message = "roomId는 필수입니다.")
        Long roomId
) {
}
