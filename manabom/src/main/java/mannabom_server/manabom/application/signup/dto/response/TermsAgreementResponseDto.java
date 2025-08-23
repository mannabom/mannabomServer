package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsAgreementResponseDto {
    private boolean success;
    private TermsAgreementDataDto data;
    private String message;

    @Getter
    @Builder
    public static class TermsAgreementDataDto {
        private boolean agreed;
    }
}