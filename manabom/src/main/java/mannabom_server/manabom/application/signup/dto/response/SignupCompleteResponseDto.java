package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupCompleteResponseDto {
    private boolean success;
    private SignupCompleteDataDto data;
    private String message;

    @Getter
    @Builder
    public static class SignupCompleteDataDto {
        private Long userId;
        private String accessToken;
        private String refreshToken;
        private int initialPoints;
    }
}