package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminAdjustWalletRequest {
    private Integer tingDelta = 0;
    private Integer eventTingDelta = 0;

    @NotBlank(message = "reason은 필수입니다.")
    private String reason;
}
