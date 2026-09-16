package mannabom_server.manabom.application.report.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mannabom_server.manabom.domain.report.entity.ReportReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void rejectsChatReportDetailLongerThanOneThousandCharacters() {
        CreateChatReportRequest request = CreateChatReportRequest.builder()
                .contextId(100L)
                .targetId(2L)
                .reason(ReportReason.ETC)
                .additionalDetail("a".repeat(1001))
                .build();

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("additionalDetail"));
    }

    @Test
    void allowsProfileReportWithoutAdditionalDetail() {
        CreateProfileReportRequest request = CreateProfileReportRequest.builder()
                .profileId(200L)
                .reason(ReportReason.INAPPROPRIATE_PROFILE)
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
