package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyEmailRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;

    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
    private String verificationCode;
}