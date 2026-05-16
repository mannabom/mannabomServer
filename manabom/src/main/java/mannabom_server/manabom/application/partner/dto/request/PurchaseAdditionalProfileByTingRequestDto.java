package mannabom_server.manabom.application.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PurchaseAdditionalProfileByTingRequestDto {
    @NotNull(message = "additionalProfileNumByTing은 필수입니다.")
    private Integer additionalProfileNumByTing;
}
