package mannabom_server.manabom.domain.currency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.currency.enums.TingBalanceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;

import java.time.Instant;

@Getter
@Entity
@Table(name = "ting_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ting_transaction_id")
    private Long tingTransactionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_type", nullable = false, updatable = false, length = 20)
    private TingBalanceType balanceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false, length = 50)
    private TingTransactionType transactionType;

    @Column(name = "amount_delta", nullable = false, updatable = false)
    private int amountDelta;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false, length = 50)
    private TingTransactionReferenceType referenceType;

    @Column(name = "reference_id", updatable = false, length = 100)
    private String referenceId;

    @Column(name = "idempotency_key", updatable = false, length = 150)
    private String idempotencyKey;

    @Column(name = "description", updatable = false, length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TingTransaction(
            Long userId,
            TingBalanceType balanceType,
            TingTransactionType transactionType,
            int amountDelta,
            int balanceAfter,
            TingTransactionReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("팅 거래 사용자 ID는 필수입니다.");
        }
        if (balanceType == null || transactionType == null) {
            throw new IllegalArgumentException("팅 거래 잔액 유형과 거래 유형은 필수입니다.");
        }
        if (amountDelta == 0 && transactionType != TingTransactionType.GIFTICON_CAPTURE) {
            throw new IllegalArgumentException("잔액 변화가 없는 팅 거래는 기록할 수 없습니다.");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("거래 후 팅 잔액은 음수일 수 없습니다.");
        }

        this.userId = userId;
        this.balanceType = balanceType;
        this.transactionType = transactionType;
        this.amountDelta = amountDelta;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = normalize(referenceId, 100, "referenceId");
        this.idempotencyKey = normalize(idempotencyKey, 150, "idempotencyKey");
        this.description = normalize(description, 500, "description");
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private String normalize(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return trimmed;
    }
}
