package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendEmailVerificationResponseDto {
    private boolean success;
    private SendEmailVerificationDataDto data;
    private String message;

    @Getter
    @Builder
    public static class SendEmailVerificationDataDto {
        private boolean emailSent;
    }
}