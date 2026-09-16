package mannabom_server.manabom.application.gifticon.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;

public record PrepareGifticonPaymentRequest(
        @NotNull(message = "gifticonProductId는 필수입니다.")
        Long gifticonProductId,
        @NotNull(message = "targetProfileId는 필수입니다.")
        Long targetProfileId,
        @Size(max = 200, message = "message는 200자를 초과할 수 없습니다.")
        String message,
        @NotNull(message = "source는 필수입니다.")
        MessageSource source
) {
}
