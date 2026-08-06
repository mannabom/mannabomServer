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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;

import java.time.Instant;

@Getter
@Entity
@Table(name = "gifticon_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GifticonPayment extends BaseTimeEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final int MAX_MESSAGE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gifticon_payment_id")
    private Long gifticonPaymentId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gifticon_product_id", nullable = false, updatable = false)
    private GifticonProduct product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_request_id", unique = true)
    private MessageRequest messageRequest;

    @Column(name = "order_id", nullable = false, unique = true, updatable = false, length = 64)
    private String orderId;

    @Column(name = "customer_key", nullable = false, updatable = false, length = 64)
    private String customerKey;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "amount", nullable = false, updatable = false)
    private int amount;

    @Column(name = "target_profile_id", nullable = false, updatable = false)
    private Long targetProfileId;

    @Column(name = "message", length = MAX_MESSAGE_LENGTH, updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_source", nullable = false, updatable = false, length = 30)
    private MessageSource messageSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GifticonPaymentStatus status;

    @Column(name = "confirmation_started_at")
    private Instant confirmationStartedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "refund_attempt_count", nullable = false)
    private int refundAttemptCount;

    @Column(name = "last_refund_attempt_at")
    private Instant lastRefundAttemptAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_creation_status", nullable = false, length = 30)
    private GifticonMessageCreationStatus messageCreationStatus;

    @Column(name = "message_creation_attempt_count", nullable = false)
    private int messageCreationAttemptCount;

    @Column(name = "last_message_creation_attempt_at")
    private Instant lastMessageCreationAttemptAt;

    @Column(name = "message_creation_failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String messageCreationFailureReason;

    public GifticonPayment(
            Long userId,
            GifticonProduct product,
            String orderId,
            String customerKey,
            Long targetProfileId,
            String message,
            MessageSource messageSource
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("결제 사용자 ID는 필수입니다.");
        }
        if (product == null || product.getSalePrice() <= 0) {
            throw new IllegalArgumentException("결제 가능한 기프티콘 상품이 필요합니다.");
        }
        if (orderId == null || !orderId.matches("[A-Za-z0-9_-]{6,64}")) {
            throw new IllegalArgumentException("토스 주문번호 형식이 올바르지 않습니다.");
        }
        if (customerKey == null || !customerKey.matches("[A-Za-z0-9_=-]{2,64}")) {
            throw new IllegalArgumentException("토스 고객 키 형식이 올바르지 않습니다.");
        }
        if (targetProfileId == null) {
            throw new IllegalArgumentException("메시지 대상 프로필 ID는 필수입니다.");
        }
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 200자를 초과할 수 없습니다.");
        }
        if (messageSource == null) {
            throw new IllegalArgumentException("메시지 출처는 필수입니다.");
        }
        this.userId = userId;
        this.product = product;
        this.orderId = orderId;
        this.customerKey = customerKey;
        this.amount = product.getSalePrice();
        this.targetProfileId = targetProfileId;
        this.message = message;
        this.messageSource = messageSource;
        this.status = GifticonPaymentStatus.READY;
        this.messageCreationStatus = GifticonMessageCreationStatus.PENDING;
    }

    public void startConfirmation(String paymentKey, Instant now, Instant staleBefore) {
        if (status == GifticonPaymentStatus.PAID) {
            if (!this.paymentKey.equals(paymentKey)) {
                throw new IllegalStateException("이미 다른 결제 키로 승인된 주문입니다.");
            }
            return;
        }
        if (status == GifticonPaymentStatus.CONFIRMING
                && confirmationStartedAt != null
                && !confirmationStartedAt.isBefore(staleBefore)) {
            throw new IllegalStateException("결제 승인 처리가 진행 중입니다.");
        }
        if (status != GifticonPaymentStatus.READY
                && status != GifticonPaymentStatus.CONFIRMING) {
            throw new IllegalStateException("승인할 수 없는 기프티콘 결제 상태입니다.");
        }
        if (this.paymentKey != null && !this.paymentKey.equals(paymentKey)) {
            throw new IllegalStateException("결제 키가 기존 승인 시도와 일치하지 않습니다.");
        }
        this.paymentKey = requireText(paymentKey, 200, "paymentKey");
        this.confirmationStartedAt = now;
        this.failureReason = null;
        this.status = GifticonPaymentStatus.CONFIRMING;
    }

    public void markPaid(Instant now) {
        if (status != GifticonPaymentStatus.CONFIRMING
                && status != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("승인 처리 중인 결제가 아닙니다.");
        }
        this.approvedAt = approvedAt == null ? now : approvedAt;
        this.failureReason = null;
        this.status = GifticonPaymentStatus.PAID;
    }

    public void markConfirmationFailed(String reason) {
        if (status == GifticonPaymentStatus.CONFIRMING) {
            this.failureReason = abbreviate(reason);
            this.status = GifticonPaymentStatus.READY;
        }
    }

    public void attachTo(MessageRequest request) {
        if (status != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("결제가 완료된 기프티콘만 메시지에 첨부할 수 있습니다.");
        }
        if (messageRequest != null) {
            throw new IllegalStateException("이미 메시지 요청에 사용된 기프티콘 결제입니다.");
        }
        if (request == null || request.getGifticonProduct() != product) {
            throw new IllegalArgumentException("결제 상품과 메시지 기프티콘 상품이 일치하지 않습니다.");
        }
        this.messageRequest = request;
        request.attachGifticonPayment(this);
        this.messageCreationFailureReason = null;
        this.messageCreationStatus = GifticonMessageCreationStatus.CREATED;
    }

    public boolean canStartMessageCreation(
            int maxAttempts,
            Instant processingStaleBefore
    ) {
        if (status != GifticonPaymentStatus.PAID
                || messageRequest != null
                || messageCreationStatus == GifticonMessageCreationStatus.CREATED
                || messageCreationStatus == GifticonMessageCreationStatus.FAILED
                || messageCreationAttemptCount >= maxAttempts) {
            return false;
        }
        return messageCreationStatus != GifticonMessageCreationStatus.PROCESSING
                || lastMessageCreationAttemptAt == null
                || lastMessageCreationAttemptAt.isBefore(processingStaleBefore);
    }

    public void startMessageCreation(Instant now) {
        if (status != GifticonPaymentStatus.PAID || messageRequest != null) {
            throw new IllegalStateException("메시지를 생성할 수 없는 기프티콘 결제 상태입니다.");
        }
        messageCreationAttemptCount++;
        lastMessageCreationAttemptAt = now;
        messageCreationFailureReason = null;
        messageCreationStatus = GifticonMessageCreationStatus.PROCESSING;
    }

    public void markMessageCreationFailed(String reason, boolean retryable, int maxAttempts) {
        if (messageCreationStatus != GifticonMessageCreationStatus.PROCESSING) {
            return;
        }
        messageCreationFailureReason = abbreviate(reason);
        messageCreationStatus = retryable && messageCreationAttemptCount < maxAttempts
                ? GifticonMessageCreationStatus.RETRY_PENDING
                : GifticonMessageCreationStatus.FAILED;
    }

    public void requestRefund() {
        if (status == GifticonPaymentStatus.REFUNDED
                || status == GifticonPaymentStatus.REFUND_PENDING
                || status == GifticonPaymentStatus.REFUND_PROCESSING
                || status == GifticonPaymentStatus.REFUND_FAILED) {
            return;
        }
        if (status != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태에서만 환불할 수 있습니다.");
        }
        this.failureReason = null;
        this.status = GifticonPaymentStatus.REFUND_PENDING;
    }

    public boolean canStartRefund(int maxAttempts, Instant staleBefore) {
        if (status == GifticonPaymentStatus.REFUNDED || refundAttemptCount >= maxAttempts) {
            return false;
        }
        if (status == GifticonPaymentStatus.REFUND_PROCESSING) {
            return lastRefundAttemptAt == null || lastRefundAttemptAt.isBefore(staleBefore);
        }
        return status == GifticonPaymentStatus.REFUND_PENDING
                || status == GifticonPaymentStatus.REFUND_FAILED;
    }

    public void startRefund(Instant now) {
        refundAttemptCount++;
        lastRefundAttemptAt = now;
        failureReason = null;
        status = GifticonPaymentStatus.REFUND_PROCESSING;
    }

    public void markRefunded(Instant now) {
        if (status != GifticonPaymentStatus.REFUND_PROCESSING
                && status != GifticonPaymentStatus.REFUNDED) {
            throw new IllegalStateException("환불 처리 중인 결제가 아닙니다.");
        }
        refundedAt = refundedAt == null ? now : refundedAt;
        failureReason = null;
        status = GifticonPaymentStatus.REFUNDED;
    }

    public void markRefundFailed(String reason) {
        if (status == GifticonPaymentStatus.REFUND_PROCESSING) {
            failureReason = abbreviate(reason);
            status = GifticonPaymentStatus.REFUND_FAILED;
        }
    }

    private String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return trimmed;
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "알 수 없는 결제 처리 오류";
        }
        return value.length() <= MAX_FAILURE_REASON_LENGTH
                ? value
                : value.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
