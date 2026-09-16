package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdminConfigureGifticonTokenRequestTest {

    @Test
    void requiresNonBlankAuditReason() {
        AdminConfigureGifticonTokenRequest request =
                new AdminConfigureGifticonTokenRequest();
        ReflectionTestUtils.setField(request, "templateToken", "token");
        ReflectionTestUtils.setField(request, "reason", " ");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .anyMatch(violation -> violation.getPropertyPath().toString()
                            .equals("reason"));
        }
    }
}
