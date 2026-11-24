package mannabom_server.manabom.domain.tingPurchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;

import java.time.Instant;

/**
 * 팅 충전 결제의 지급 상세. {@link Payment} 와 PK 를 공유하는 1:1.
 * 결제 수단이 무엇이든(스토어/토스) 팅을 얼마나 지급하고 회수했는지만 다룬다.
 */
@Getter
@Entity
@Table(name = "ting_purchase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TingPurchase extends BaseTimeEntity {

    @Id
    @Column(name = "payment_id")
    private Long paymentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ting_product_id", nullable = false, updatable = false)
    private TingProduct product;

    /** 1개 구매당 지급 팅(상품 스냅샷) */
    @Column(name = "ting_amount", nullable = false, updatable = false)
    private int tingAmount;

    @Column(name = "bonus_event_ting", nullable = false, updatable = false)
    private int bonusEventTing;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "credited_ting", nullable = false)
    private int creditedTing;

    @Column(name = "credited_event_ting", nullable = false)
    private int creditedEventTing;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @Column(name = "clawed_back_ting", nullable = false)
    private int clawedBackTing;

    @Column(name = "clawed_back_event_ting", nullable = false)
    private int clawedBackEventTing;

    @Column(name = "clawed_back_at")
    private Instant clawedBackAt;

    public TingPurchase(Payment payment, TingProduct product) {
        if (payment == null) {
            throw new IllegalArgumentException("결제 원장은 필수입니다.");
        }
        if (payment.getPurpose() != PaymentPurpose.TING_CHARGE) {
            throw new IllegalArgumentException("팅 충전 목적의 결제만 연결할 수 있습니다.");
        }
        if (product == null) {
            throw new IllegalArgumentException("팅 상품은 필수입니다.");
        }
        this.payment = payment;
        this.product = product;
        this.tingAmount = product.getTingAmount();
        this.bonusEventTing = product.getBonusEventTing();
        this.quantity = 1;
    }

    public boolean isCredited() {
        return creditedAt != null;
    }

    public int expectedTing(int quantity) {
        return tingAmount * quantity;
    }

    public int expectedEventTing(int quantity) {
        return bonusEventTing * quantity;
    }

    /** 지갑 반영 결과를 기록한다. 이미 지급됐으면 무시(멱등). */
    public void markCredited(int quantity, Instant now) {
        if (isCredited()) {
            return;
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 0보다 커야 합니다.");
        }
        this.quantity = quantity;
        this.creditedTing = expectedTing(quantity);
        this.creditedEventTing = expectedEventTing(quantity);
        this.creditedAt = now;
    }

    /** 환불 회수 결과를 기록한다. 실제 회수량은 지갑 잔액에 따라 지급량보다 적을 수 있다. */
    public void markClawedBack(int clawedBackTing, int clawedBackEventTing, Instant now) {
        if (!isCredited()) {
            throw new IllegalStateException("팅이 지급된 구매만 회수 처리할 수 있습니다.");
        }
        if (clawedBackAt != null) {
            return;
        }
        if (clawedBackTing < 0 || clawedBackTing > creditedTing
                || clawedBackEventTing < 0 || clawedBackEventTing > creditedEventTing) {
            throw new IllegalArgumentException("회수 팅 수량이 지급량 범위를 벗어났습니다.");
        }
        this.clawedBackTing = clawedBackTing;
        this.clawedBackEventTing = clawedBackEventTing;
        this.clawedBackAt = now;
    }

    public boolean hasClawbackShortfall() {
        return clawedBackAt != null
                && (clawedBackTing < creditedTing || clawedBackEventTing < creditedEventTing);
    }
}
