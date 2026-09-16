package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminGifticonPaymentActionRequest(
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 500, message = "reason은 500자를 초과할 수 없습니다.")
        String reason
) {
}
