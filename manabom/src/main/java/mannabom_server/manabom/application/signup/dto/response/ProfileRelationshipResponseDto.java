package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileRelationshipResponseDto {
    private boolean success;
    private ProfileRelationshipDataDto data;
    private String message;

    @Getter
    @Builder
    public static class ProfileRelationshipDataDto {
        private String profileId;
    }
}