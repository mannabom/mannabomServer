package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsContentResponseDto {
    private boolean success;
    private TermsContentDataDto data;
    private String message;

    @Getter
    @Builder
    public static class TermsContentDataDto {
        private String termType;
        private String title;
        private String content;
        private String lastUpdated;
        private boolean required;
    }
}