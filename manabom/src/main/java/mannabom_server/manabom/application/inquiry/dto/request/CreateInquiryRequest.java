package mannabom_server.manabom.application.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import mannabom_server.manabom.domain.inquiry.enums.InquiryCategory;

@Getter
public class CreateInquiryRequest {
    @NotNull
    private InquiryCategory category;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private Long relatedChatRoomId;
    private Long relatedMeetingId;
    private String relatedPaymentId;
}
