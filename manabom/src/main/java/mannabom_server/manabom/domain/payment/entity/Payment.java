package mannabom_server.manabom.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.payment.enums.PaymentProvider;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 결제 공통 원장. 결제 수단(provider)과 목적(purpose)에 무관한 "돈" 정보만 담는다.
 * 수단별 상세는 {@link StorePayment} 등 PK 를 공유하는 1:1 테이블에 둔다.
 */
@Getter
@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final int MAX_REFUND_REASON_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 30)
    private PaymentPurpose purpose;

    @Column(name = "amount", precision = 18, scale = 6)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", length = 20)
    private StoreEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refund_reason", length = MAX_REFUND_REASON_LENGTH)
    private String refundReason;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    private Payment(Long userId, PaymentProvider provider, PaymentPurpose purpose, PaymentStatus initialStatus) {
        if (userId == null) {
            throw new IllegalArgumentException("결제 사용자 ID는 필수입니다.");
        }
        if (provider == null || purpose == null) {
            throw new IllegalArgumentException("결제 수단과 결제 목적은 필수입니다.");
        }
        this.userId = userId;
        this.provider = provider;
        this.purpose = purpose;
        this.status = initialStatus;
    }

    /** 스토어 인앱결제처럼 클라이언트가 먼저 결제하고 서버가 검증하는 흐름의 시작 상태. */
    public static Payment startVerifying(Long userId, PaymentProvider provider, PaymentPurpose purpose) {
        return new Payment(userId, provider, purpose, PaymentStatus.VERIFYING);
    }

    /** 토스처럼 서버가 주문을 먼저 만들고 승인하는 흐름의 시작 상태. (기프티콘 이관 시 사용) */
    public static Payment ready(Long userId, PaymentProvider provider, PaymentPurpose purpose) {
        return new Payment(userId, provider, purpose, PaymentStatus.READY);
    }

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    /** 결제 확정. 이미 PAID 이면 무시(멱등). */
    public void markPaid(BigDecimal amount, String currency, StoreEnvironment environment, Instant now) {
        if (status == PaymentStatus.PAID) {
            return;
        }
        if (status != PaymentStatus.VERIFYING && status != PaymentStatus.READY) {
            throw new IllegalStateException("검증/승인 진행 중인 결제만 확정할 수 있습니다.");
        }
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("결제 금액은 음수일 수 없습니다.");
        }
        this.amount = amount;
        this.currency = currency == null ? null : truncate(currency.trim(), 10);
        this.environment = environment;
        this.paidAt = now;
        this.failureReason = null;
        this.status = PaymentStatus.PAID;
    }

    /** 결제사가 유효하지 않다고 판정한 경우(영구 실패). */
    public void markFailed(String reason, Instant now) {
        if (status != PaymentStatus.VERIFYING && status != PaymentStatus.READY) {
            throw new IllegalStateException("검증/승인 진행 중인 결제만 실패 처리할 수 있습니다.");
        }
        this.failureReason = abbreviate(reason);
        this.failedAt = now;
        this.status = PaymentStatus.FAILED;
    }

    /** 결제사 통신 장애 등 일시적 실패. 상태는 유지하고 사유만 기록한다. */
    public void recordTransientFailure(String reason) {
        if (status == PaymentStatus.VERIFYING || status == PaymentStatus.READY) {
            this.failureReason = abbreviate(reason);
        }
    }

    /**
     * 결제사가 환불을 확정했음을 반영한다(스토어 인앱결제처럼 결제사 주도 환불).
     *
     * @return 이번 호출로 상태가 바뀌었으면 true (이미 환불 처리된 건이면 false)
     */
    public boolean markRefunded(String reason, Instant refundedAt) {
        if (status == PaymentStatus.REFUNDED) {
            return false;
        }
        if (status != PaymentStatus.PAID
                && status != PaymentStatus.REFUND_PENDING
                && status != PaymentStatus.REFUND_PROCESSING
                && status != PaymentStatus.REFUND_FAILED) {
            throw new IllegalStateException("결제 완료 상태에서만 환불 처리할 수 있습니다.");
        }
        this.refundReason = reason == null ? null : truncate(reason.trim(), MAX_REFUND_REASON_LENGTH);
        this.refundedAt = refundedAt == null ? Instant.now() : refundedAt;
        this.failureReason = null;
        this.status = PaymentStatus.REFUNDED;
        return true;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "알 수 없는 결제 처리 오류";
        }
        return truncate(value, MAX_FAILURE_REASON_LENGTH);
    }
}
