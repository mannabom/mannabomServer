package mannabom_server.manabom.application.tingPurchase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchase;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchaseState;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.domain.payment.repository.PaymentRepository;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingProductRepository;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import mannabom_server.manabom.policy.config.TingPurchaseProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 팅 인앱결제 상태 전이. 각 메서드는 독립 트랜잭션(REQUIRES_NEW)으로 커밋되어
 * 외부 스토어 호출 전후의 상태가 반드시 남도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TingPurchaseStateService {

    private static final String CREDIT_IDEMPOTENCY_PREFIX = "TING_PURCHASE_CREDIT_";
    private static final String CREDIT_EVENT_IDEMPOTENCY_PREFIX = "TING_PURCHASE_CREDIT_EVENT_";

    private final PaymentRepository paymentRepository;
    private final StorePaymentRepository storePaymentRepository;
    private final TingPurchaseRepository tingPurchaseRepository;
    private final TingProductRepository productRepository;
    private final TingWalletRepository tingWalletRepository;
    private final TingTransactionRecorder tingTransactionRecorder;
    private final TingPurchaseProperties properties;

    /**
     * (store, purchaseKey) 로 기존 구매를 찾거나 VERIFYING 상태의 새 구매를 만든다.
     * 동시 요청으로 unique 제약에 걸리면 호출측이 다시 begin 을 호출한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationAttempt begin(Long userId, PurchaseStore store, String productCode, String purchaseKey) {
        if (userId == null) {
            throw new IllegalArgumentException("결제 사용자 정보가 필요합니다.");
        }
        if (store == null) {
            throw new IllegalArgumentException("스토어 정보는 필수입니다.");
        }
        if (purchaseKey == null || purchaseKey.isBlank()) {
            throw new IllegalArgumentException(store == PurchaseStore.APPLE
                    ? "Apple 결제는 transactionId가 필요합니다."
                    : "Google 결제는 purchaseToken이 필요합니다.");
        }
        String key = purchaseKey.trim();

        StorePayment existing = storePaymentRepository
                .findByStoreAndPurchaseKeyForUpdate(store, key)
                .orElse(null);
        if (existing != null) {
            if (!userId.equals(existing.getPayment().getUserId())) {
                throw new IllegalArgumentException("다른 사용자가 이미 등록한 스토어 구매입니다.");
            }
            return VerificationAttempt.of(existing);
        }

        TingProduct product = productRepository.findByProductCode(productCode == null ? "" : productCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("팅 상품을 찾을 수 없습니다."));
        if (!product.isPurchasableAt(store)) {
            throw new IllegalStateException("현재 " + store.getDescription() + "에서 구매할 수 없는 팅 상품입니다.");
        }

        Payment payment = paymentRepository.save(
                Payment.startVerifying(userId, store.getProvider(), PaymentPurpose.TING_CHARGE)
        );
        StorePayment storePayment = storePaymentRepository.save(
                new StorePayment(payment, store, product.storeProductId(store), key)
        );
        tingPurchaseRepository.save(new TingPurchase(payment, product));
        return VerificationAttempt.of(storePayment);
    }

    /**
     * 스토어 검증 결과를 반영한다. 통과하면 결제를 PAID 로 바꾸고 지갑에 팅을 넣는다.
     * 거절 사유가 있으면 FAILED 로 바꾸고 REJECTED 결과를 돌려준다(예외를 던지면 롤백되므로 결과로 전달).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreditOutcome credit(Long paymentId, StorePurchase result) {
        StorePayment storePayment = findForUpdate(paymentId);
        Payment payment = storePayment.getPayment();
        Instant now = Instant.now();

        if (payment.isPaid() || payment.isRefunded()) {
            return CreditOutcome.alreadyCredited();
        }
        if (payment.getStatus() != PaymentStatus.VERIFYING) {
            return CreditOutcome.rejected("검증할 수 없는 결제 상태입니다: " + payment.getStatus());
        }

        String rejection = validate(storePayment, result);
        if (rejection != null) {
            payment.markFailed(rejection, now);
            return CreditOutcome.rejected(rejection);
        }
        if (result.state() == StorePurchaseState.PENDING) {
            payment.recordTransientFailure("스토어 결제가 아직 완료되지 않았습니다.");
            return CreditOutcome.pending();
        }

        TingPurchase tingPurchase = tingPurchaseRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalStateException("팅 지급 정보를 찾을 수 없습니다."));
        int quantity = Math.max(1, result.quantity());
        boolean consumeRequired = storePayment.getStore() == PurchaseStore.GOOGLE && !result.alreadyConsumed();

        storePayment.applyVerification(
                result.transactionId(),
                result.originalTransactionId(),
                quantity,
                result.purchasedAt(),
                result.accountToken(),
                consumeRequired,
                result.rawPayload()
        );
        payment.markPaid(result.amount(), result.currency(), result.environment(), now);

        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(payment.getUserId())
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(payment.getUserId())));
        int ting = tingPurchase.expectedTing(quantity);
        int eventTing = tingPurchase.expectedEventTing(quantity);
        String description = tingPurchase.getProduct().getName() + " 인앱결제 (" + storePayment.getStore().getDescription() + ")";

        wallet.addTing(ting);
        tingTransactionRecorder.recordPaid(
                wallet,
                TingTransactionType.TING_PURCHASE,
                ting,
                TingTransactionReferenceType.TING_PURCHASE,
                String.valueOf(paymentId),
                CREDIT_IDEMPOTENCY_PREFIX + paymentId,
                description
        );
        if (eventTing > 0) {
            wallet.addEventTing(eventTing);
            tingTransactionRecorder.recordEvent(
                    wallet,
                    TingTransactionType.TING_PURCHASE,
                    eventTing,
                    TingTransactionReferenceType.TING_PURCHASE,
                    String.valueOf(paymentId),
                    CREDIT_EVENT_IDEMPOTENCY_PREFIX + paymentId,
                    description + " 보너스"
            );
        }
        tingPurchase.markCredited(quantity, now);
        return CreditOutcome.credited(ting, eventTing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long paymentId, String reason) {
        Payment payment = findForUpdate(paymentId).getPayment();
        if (payment.getStatus() == PaymentStatus.VERIFYING) {
            payment.markFailed(reason, Instant.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransientFailure(Long paymentId, String reason) {
        findForUpdate(paymentId).getPayment().recordTransientFailure(reason);
    }

    /** consume 시도를 시작한다. 필요 없거나 재시도 한도를 넘었으면 null. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsumeAttempt startConsume(Long paymentId) {
        StorePayment storePayment = findForUpdate(paymentId);
        Instant now = Instant.now();
        if (!storePayment.canStartConsume(
                Math.max(1, properties.getConsumeRetry().getMaxAttempts()),
                now.minusSeconds(staleSeconds())
        )) {
            return null;
        }
        storePayment.startConsume(now);
        return new ConsumeAttempt(paymentId, storePayment.getStore(), storePayment.getStoreProductId(), storePayment.getPurchaseKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeConsume(Long paymentId) {
        findForUpdate(paymentId).markConsumed(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failConsume(Long paymentId) {
        findForUpdate(paymentId).markConsumeFailed();
    }

    public long staleSeconds() {
        return Math.max(60L, properties.getRequestTimeoutSeconds() * 3L);
    }

    private String validate(StorePayment storePayment, StorePurchase result) {
        if (result == null) {
            return "스토어가 구매 정보를 반환하지 않았습니다.";
        }
        if (result.store() != storePayment.getStore()) {
            return "요청한 스토어와 검증 결과의 스토어가 다릅니다.";
        }
        if (result.storeProductId() == null
                || !result.storeProductId().equals(storePayment.getStoreProductId())) {
            return "스토어 상품 ID가 요청한 팅 상품과 일치하지 않습니다: " + result.storeProductId();
        }
        if (result.state() == StorePurchaseState.CANCELED) {
            return "스토어에서 취소 또는 환불된 구매입니다.";
        }
        if (result.state() == StorePurchaseState.PURCHASED
                && (result.transactionId() == null || result.transactionId().isBlank())) {
            return "스토어 거래 번호가 없습니다.";
        }
        if (result.environment() == StoreEnvironment.SANDBOX && !properties.isAllowSandboxCredit()) {
            return "샌드박스(테스트) 구매는 팅을 지급하지 않습니다.";
        }
        Long userId = storePayment.getPayment().getUserId();
        if (result.accountToken() != null && !result.accountToken().isBlank()) {
            if (!PurchaseAccountToken.matches(userId, result.accountToken())) {
                return "스토어 구매자 토큰이 요청 사용자와 일치하지 않습니다.";
            }
        } else if (properties.isRequireAccountToken()) {
            return "스토어 구매자 토큰이 없어 구매자를 확인할 수 없습니다.";
        }
        return null;
    }

    private StorePayment findForUpdate(Long paymentId) {
        return storePaymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("스토어 결제를 찾을 수 없습니다."));
    }

    public record VerificationAttempt(
            Long paymentId,
            PaymentStatus status,
            PurchaseStore store,
            String storeProductId,
            String purchaseKey,
            String failureReason
    ) {
        static VerificationAttempt of(StorePayment storePayment) {
            Payment payment = storePayment.getPayment();
            return new VerificationAttempt(
                    payment.getPaymentId(),
                    payment.getStatus(),
                    storePayment.getStore(),
                    storePayment.getStoreProductId(),
                    storePayment.getPurchaseKey(),
                    payment.getFailureReason()
            );
        }
    }

    public record ConsumeAttempt(
            Long paymentId,
            PurchaseStore store,
            String storeProductId,
            String purchaseKey
    ) {
    }

    public record CreditOutcome(Result result, String reason, int creditedTing, int creditedEventTing) {
        public enum Result { CREDITED, ALREADY_CREDITED, PENDING, REJECTED }

        static CreditOutcome credited(int ting, int eventTing) {
            return new CreditOutcome(Result.CREDITED, null, ting, eventTing);
        }

        static CreditOutcome alreadyCredited() {
            return new CreditOutcome(Result.ALREADY_CREDITED, null, 0, 0);
        }

        static CreditOutcome pending() {
            return new CreditOutcome(Result.PENDING, null, 0, 0);
        }

        static CreditOutcome rejected(String reason) {
            return new CreditOutcome(Result.REJECTED, reason, 0, 0);
        }

        public boolean isCredited() {
            return result == Result.CREDITED || result == Result.ALREADY_CREDITED;
        }
    }
}
