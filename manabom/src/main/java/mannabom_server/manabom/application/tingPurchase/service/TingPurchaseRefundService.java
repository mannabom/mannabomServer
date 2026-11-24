package mannabom_server.manabom.application.tingPurchase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * 스토어가 확정한 환불(Apple 서버 알림, Google voided purchases)을 반영하고 지급한 팅을 회수한다.
 * 잔액이 부족하면 남은 만큼만 회수하고 부족분은 기록으로 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TingPurchaseRefundService {

    private static final String REFUND_IDEMPOTENCY_PREFIX = "TING_PURCHASE_REFUND_";
    private static final String REFUND_EVENT_IDEMPOTENCY_PREFIX = "TING_PURCHASE_REFUND_EVENT_";

    private final StorePaymentRepository storePaymentRepository;
    private final TingPurchaseRepository tingPurchaseRepository;
    private final TingWalletRepository tingWalletRepository;
    private final TingTransactionRecorder tingTransactionRecorder;

    /** Apple: transactionId 로 조회 */
    @Transactional
    public RefundOutcome refundByTransactionId(PurchaseStore store, String transactionId, String reason, Instant refundedAt) {
        if (transactionId == null || transactionId.isBlank()) {
            return RefundOutcome.NOT_FOUND;
        }
        return refund(
                storePaymentRepository.findByStoreAndTransactionIdForUpdate(store, transactionId.trim()),
                store, transactionId, reason, refundedAt
        );
    }

    /** Google: purchaseToken 으로 조회 */
    @Transactional
    public RefundOutcome refundByPurchaseKey(PurchaseStore store, String purchaseKey, String reason, Instant refundedAt) {
        if (purchaseKey == null || purchaseKey.isBlank()) {
            return RefundOutcome.NOT_FOUND;
        }
        return refund(
                storePaymentRepository.findByStoreAndPurchaseKeyForUpdate(store, purchaseKey.trim()),
                store, purchaseKey, reason, refundedAt
        );
    }

    private RefundOutcome refund(
            Optional<StorePayment> found,
            PurchaseStore store,
            String identifier,
            String reason,
            Instant refundedAt
    ) {
        StorePayment storePayment = found.orElse(null);
        if (storePayment == null) {
            log.warn("환불 알림에 해당하는 스토어 결제가 없습니다. store={}, identifier={}", store, abbreviate(identifier));
            return RefundOutcome.NOT_FOUND;
        }
        Payment payment = storePayment.getPayment();
        Long paymentId = payment.getPaymentId();
        Instant now = Instant.now();

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return RefundOutcome.ALREADY_REFUNDED;
        }
        if (payment.getStatus() == PaymentStatus.VERIFYING) {
            payment.markFailed("지급 전 스토어 환불 확인: " + reason, now);
            log.info("지급 전 환불 확인으로 결제를 실패 처리. paymentId={}", paymentId);
            return RefundOutcome.FAILED_BEFORE_CREDIT;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return RefundOutcome.NOT_CREDITED;
        }

        TingPurchase tingPurchase = tingPurchaseRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalStateException("팅 지급 정보를 찾을 수 없습니다."));
        payment.markRefunded(reason, refundedAt);

        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(payment.getUserId())
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(payment.getUserId())));
        int clawTing = Math.min(wallet.getTing(), tingPurchase.getCreditedTing());
        int clawEventTing = Math.min(wallet.getEventTing(), tingPurchase.getCreditedEventTing());
        String description = "스토어 환불 회수 (" + store.getDescription() + ")"
                + (reason == null || reason.isBlank() ? "" : ": " + reason);

        if (clawTing > 0) {
            wallet.spendTing(clawTing);
            tingTransactionRecorder.recordPaid(
                    wallet,
                    TingTransactionType.TING_PURCHASE_REFUND,
                    -clawTing,
                    TingTransactionReferenceType.TING_PURCHASE,
                    String.valueOf(paymentId),
                    REFUND_IDEMPOTENCY_PREFIX + paymentId,
                    description
            );
        }
        if (clawEventTing > 0) {
            wallet.spendEventTing(clawEventTing);
            tingTransactionRecorder.recordEvent(
                    wallet,
                    TingTransactionType.TING_PURCHASE_REFUND,
                    -clawEventTing,
                    TingTransactionReferenceType.TING_PURCHASE,
                    String.valueOf(paymentId),
                    REFUND_EVENT_IDEMPOTENCY_PREFIX + paymentId,
                    description
            );
        }
        tingPurchase.markClawedBack(clawTing, clawEventTing, now);

        if (tingPurchase.hasClawbackShortfall()) {
            log.warn("환불 회수 부족. paymentId={}, userId={}, credited={}/{}, clawedBack={}/{}",
                    paymentId, payment.getUserId(),
                    tingPurchase.getCreditedTing(), tingPurchase.getCreditedEventTing(),
                    clawTing, clawEventTing);
            return RefundOutcome.REFUNDED_WITH_SHORTFALL;
        }
        return RefundOutcome.REFUNDED;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 24 ? value : value.substring(0, 24) + "...";
    }

    public enum RefundOutcome {
        REFUNDED,
        REFUNDED_WITH_SHORTFALL,
        ALREADY_REFUNDED,
        FAILED_BEFORE_CREDIT,
        NOT_CREDITED,
        NOT_FOUND
    }
}
