package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetNicknameResponseDto {
    private boolean success;
    private SetNicknameDataDto data;
    private String message;

    @Getter
    @Builder
    public static class SetNicknameDataDto {
        private String profileId;
    }
}