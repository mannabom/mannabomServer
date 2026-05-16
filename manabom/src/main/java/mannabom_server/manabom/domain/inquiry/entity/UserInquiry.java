package mannabom_server.manabom.domain.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.inquiry.enums.InquiryCategory;
import mannabom_server.manabom.domain.inquiry.enums.InquiryStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInquiry extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private InquiryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InquiryStatus status;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_chat_room_id")
    private Long relatedChatRoomId;

    @Column(name = "related_meeting_id")
    private Long relatedMeetingId;

    @Column(name = "related_payment_id")
    private String relatedPaymentId;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_by_admin_id")
    private Long answeredByAdminId;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    public UserInquiry(Long userId, InquiryCategory category, String title, String content,
                       Long relatedChatRoomId, Long relatedMeetingId, String relatedPaymentId) {
        this.userId = userId;
        this.category = category;
        this.status = InquiryStatus.OPEN;
        this.title = title;
        this.content = content;
        this.relatedChatRoomId = relatedChatRoomId;
        this.relatedMeetingId = relatedMeetingId;
        this.relatedPaymentId = relatedPaymentId;
    }

    public void answer(String answer, Long adminId) {
        this.answer = answer;
        this.answeredByAdminId = adminId;
        this.answeredAt = LocalDateTime.now();
        this.status = InquiryStatus.ANSWERED;
    }

    public void close() {
        this.status = InquiryStatus.CLOSED;
    }
}
