package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendEmailVerificationRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(regexp = ".*\\.ac\\.kr$", message = "대학 이메일(.ac.kr)만 사용 가능합니다.")
    private String email;
}
