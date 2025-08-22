package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameCheckResponseDto {
    private boolean success;
    private NicknameCheckDataDto data;
    private String message;

    @Getter
    @Builder
    public static class NicknameCheckDataDto {
        private boolean available;
    }
}
