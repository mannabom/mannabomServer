package mannabom_server.manabom.application.inquiry.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.inquiry.enums.InquiryCategory;
import mannabom_server.manabom.domain.inquiry.enums.InquiryStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class InquiryResponse {
    private final Long inquiryId;
    private final Long userId;
    private final InquiryCategory category;
    private final InquiryStatus status;
    private final String title;
    private final String content;
    private final Long relatedChatRoomId;
    private final Long relatedMeetingId;
    private final String relatedPaymentId;
    private final String answer;
    private final Long answeredByAdminId;
    private final LocalDateTime answeredAt;
    private final Instant createdAt;
    private final Map<String, Object> relatedContext;
}
