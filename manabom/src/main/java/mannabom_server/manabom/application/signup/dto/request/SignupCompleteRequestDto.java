package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupCompleteRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;
}