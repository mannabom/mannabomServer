package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermsAgreementRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;

    @Valid
    @NotNull(message = "약관 동의 정보는 필수입니다.")
    private TermsAgreementDto termsAgreement;

    @Getter
    @NoArgsConstructor
    public static class TermsAgreementDto {
        @NotNull(message = "서비스 이용약관 동의는 필수입니다.")
        private Boolean serviceTerms;

        @NotNull(message = "개인정보 처리방침 동의는 필수입니다.")
        private Boolean privacyPolicy;

        private Boolean marketingConsent; // 선택사항
    }
}
