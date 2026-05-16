package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminUpdatePolicyRequest {
    @NotBlank(message = "key는 필수입니다.")
    private String key;

    private Integer value;

    private boolean resetToDefault;

    @NotBlank(message = "reason은 필수입니다.")
    private String reason;
}
