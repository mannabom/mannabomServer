package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminConfigureGifticonTokenRequest {

    @NotBlank(message = "templateToken은 필수입니다.")
    @Size(max = 512, message = "templateToken은 512자를 초과할 수 없습니다.")
    private String templateToken;

    @NotBlank(message = "reason은 필수입니다.")
    @Size(max = 500, message = "reason은 500자를 초과할 수 없습니다.")
    private String reason;
}
