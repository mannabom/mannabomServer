package mannabom_server.manabom.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;

import java.time.Instant;

/**
 * Apple App Store / Google Play 인앱결제 상세. {@link Payment} 와 PK 를 공유하는 1:1.
 */
@Getter
@Entity
@Table(name = "store_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorePayment extends BaseTimeEntity {

    private static final int MAX_PURCHASE_KEY_LENGTH = 1000;

    @Id
    @Column(name = "payment_id")
    private Long paymentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", nullable = false, updatable = false, length = 20)
    private PurchaseStore store;

    @Column(name = "store_product_id", nullable = false, updatable = false, length = 100)
    private String storeProductId;

    /** 클라이언트가 보낸 멱등 키. Apple: transactionId, Google: purchaseToken */
    @Column(name = "purchase_key", nullable = false, updatable = false, length = MAX_PURCHASE_KEY_LENGTH)
    private String purchaseKey;

    /** 검증 후 확정된 거래 번호. Apple: transactionId, Google: orderId */
    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    @Column(name = "original_transaction_id", length = 200)
    private String originalTransactionId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Apple appAccountToken / Google obfuscatedExternalAccountId */
    @Column(name = "account_token", length = 100)
    private String accountToken;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "consume_status", nullable = false, length = 30)
    private StoreConsumeStatus consumeStatus;

    @Column(name = "consume_attempt_count", nullable = false)
    private int consumeAttemptCount;

    @Column(name = "last_consume_attempt_at")
    private Instant lastConsumeAttemptAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    /** 스토어가 준 원문(Apple: 서명된 JWS, Google: ProductPurchase JSON). 감사/분쟁 대응용 */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    public StorePayment(Payment payment, PurchaseStore store, String storeProductId, String purchaseKey) {
        if (payment == null) {
            throw new IllegalArgumentException("결제 원장은 필수입니다.");
        }
        if (store == null) {
            throw new IllegalArgumentException("스토어 정보는 필수입니다.");
        }
        if (payment.getProvider() != store.getProvider()) {
            throw new IllegalArgumentException("결제 원장의 결제 수단과 스토어가 일치하지 않습니다.");
        }
        if (storeProductId == null || storeProductId.isBlank() || storeProductId.length() > 100) {
            throw new IllegalArgumentException("스토어 상품 ID는 1~100자여야 합니다.");
        }
        if (purchaseKey == null || purchaseKey.isBlank()) {
            throw new IllegalArgumentException("스토어 구매 식별자는 필수입니다.");
        }
        String key = purchaseKey.trim();
        if (key.length() > MAX_PURCHASE_KEY_LENGTH) {
            throw new IllegalArgumentException("스토어 구매 식별자는 1000자를 초과할 수 없습니다.");
        }
        this.payment = payment;
        this.store = store;
        this.storeProductId = storeProductId.trim();
        this.purchaseKey = key;
        this.quantity = 1;
        this.consumeStatus = StoreConsumeStatus.NOT_REQUIRED;
    }

    /** 스토어 검증 결과를 기록한다. */
    public void applyVerification(
            String transactionId,
            String originalTransactionId,
            int quantity,
            Instant purchasedAt,
            String accountToken,
            boolean consumeRequired,
            String payload
    ) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("스토어 거래 번호는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 0보다 커야 합니다.");
        }
        if (this.transactionId != null && !this.transactionId.equals(transactionId.trim())) {
            throw new IllegalStateException("이미 다른 거래 번호로 검증된 구매입니다.");
        }
        this.transactionId = truncate(transactionId.trim(), 200);
        this.originalTransactionId = originalTransactionId == null
                ? null : truncate(originalTransactionId.trim(), 200);
        this.quantity = quantity;
        this.purchasedAt = purchasedAt;
        this.accountToken = accountToken == null ? null : truncate(accountToken, 100);
        this.payload = payload;
        if (consumeStatus == StoreConsumeStatus.NOT_REQUIRED && consumeRequired) {
            this.consumeStatus = StoreConsumeStatus.PENDING;
        }
    }

    public boolean canStartConsume(int maxAttempts, Instant processingStaleBefore) {
        if (!payment.isPaid()
                || consumeStatus == StoreConsumeStatus.NOT_REQUIRED
                || consumeStatus == StoreConsumeStatus.DONE
                || consumeAttemptCount >= maxAttempts) {
            return false;
        }
        if (consumeStatus == StoreConsumeStatus.PROCESSING) {
            return lastConsumeAttemptAt == null || lastConsumeAttemptAt.isBefore(processingStaleBefore);
        }
        return true;
    }

    public void startConsume(Instant now) {
        if (!payment.isPaid()
                || consumeStatus == StoreConsumeStatus.NOT_REQUIRED
                || consumeStatus == StoreConsumeStatus.DONE) {
            throw new IllegalStateException("스토어 소모 처리를 시작할 수 없는 구매 상태입니다.");
        }
        consumeAttemptCount++;
        lastConsumeAttemptAt = now;
        consumeStatus = StoreConsumeStatus.PROCESSING;
    }

    public void markConsumed(Instant now) {
        if (consumeStatus == StoreConsumeStatus.NOT_REQUIRED) {
            throw new IllegalStateException("스토어 소모 처리가 필요하지 않은 구매입니다.");
        }
        consumedAt = consumedAt == null ? now : consumedAt;
        consumeStatus = StoreConsumeStatus.DONE;
    }

    public void markConsumeFailed() {
        if (consumeStatus == StoreConsumeStatus.PROCESSING) {
            consumeStatus = StoreConsumeStatus.FAILED;
        }
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
