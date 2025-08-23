package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyEmailResponseDto {
    private boolean success;
    private VerifyEmailDataDto data;
    private String message;

    @Getter
    @Builder
    public static class VerifyEmailDataDto {
        private boolean verified;
    }
}