package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminTingProductRequest(
        @Pattern(regexp = "[A-Z0-9_]{2,50}", message = "productCode는 대문자/숫자/밑줄 2~50자여야 합니다.")
        String productCode,
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100)
        String name,
        @Positive(message = "tingAmount는 0보다 커야 합니다.")
        int tingAmount,
        @Min(value = 0, message = "bonusEventTing은 0 이상이어야 합니다.")
        int bonusEventTing,
        @Min(value = 0, message = "displayPriceKrw는 0 이상이어야 합니다.")
        int displayPriceKrw,
        @Size(max = 100)
        String appleProductId,
        @Size(max = 100)
        String googleProductId,
        @NotNull(message = "active는 필수입니다.")
        Boolean active,
        int sortOrder,
        @Size(max = 500)
        String reason
) {
}
