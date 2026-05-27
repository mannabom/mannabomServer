package mannabom_server.manabom.application.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AnswerInquiryRequest {
    @NotBlank
    private String answer;
}
