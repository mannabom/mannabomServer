package mannabom_server.manabom.domain.gifticon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;

import java.time.Instant;

@Getter
@Entity
@Table(name = "gifticon_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GifticonOrder extends BaseTimeEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gifticon_order_id")
    private Long gifticonOrderId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_request_id", nullable = false, unique = true)
    private MessageRequest messageRequest;

    @Column(name = "encrypted_template_token", nullable = false, length = 1024)
    private String encryptedTemplateToken;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "external_key", nullable = false, unique = true, length = 70)
    private String externalKey;

    @Column(name = "external_order_id", nullable = false, unique = true, length = 70)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GifticonOrderStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    public GifticonOrder(
            MessageRequest messageRequest,
            String encryptedTemplateToken,
            String receiverPhone,
            String receiverName,
            String externalKey,
            String externalOrderId
    ) {
        if (messageRequest == null || messageRequest.getGifticonProduct() == null) {
            throw new IllegalArgumentException("기프티콘이 연결된 메시지 요청이 필요합니다.");
        }
        if (encryptedTemplateToken == null || encryptedTemplateToken.isBlank()) {
            throw new IllegalArgumentException("암호화된 템플릿 토큰은 필수입니다.");
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            throw new IllegalArgumentException("수신자 휴대폰 번호는 필수입니다.");
        }
        if (receiverName == null || receiverName.isBlank()) {
            throw new IllegalArgumentException("수신자 이름은 필수입니다.");
        }
        this.messageRequest = messageRequest;
        this.encryptedTemplateToken = encryptedTemplateToken;
        this.receiverPhone = receiverPhone;
        this.receiverName = receiverName;
        this.externalKey = requireExternalId(externalKey, "externalKey");
        this.externalOrderId = requireExternalId(externalOrderId, "externalOrderId");
        this.status = GifticonOrderStatus.PENDING;
    }

    public boolean canAttempt(int maxAttempts) {
        return status != GifticonOrderStatus.REQUESTED && attemptCount < maxAttempts;
    }

    public void markRequested(Instant now) {
        attemptCount++;
        lastAttemptAt = now;
        requestedAt = now;
        failureReason = null;
        status = GifticonOrderStatus.REQUESTED;
    }

    public void markFailed(Instant now, String reason) {
        attemptCount++;
        lastAttemptAt = now;
        failureReason = abbreviate(reason);
        status = GifticonOrderStatus.FAILED;
    }

    private static String requireExternalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 70) {
            throw new IllegalArgumentException(fieldName + "는 70자를 초과할 수 없습니다.");
        }
        return trimmed;
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "알 수 없는 발송 요청 오류";
        }
        return value.length() <= MAX_FAILURE_REASON_LENGTH
                ? value
                : value.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
