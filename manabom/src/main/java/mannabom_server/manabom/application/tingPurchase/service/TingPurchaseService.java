package mannabom_server.manabom.application.tingPurchase.service;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.dto.request.VerifyTingPurchaseRequest;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingPurchaseResponse;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.ConsumePurchaseCommand;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchase;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.VerifyPurchaseCommand;
import mannabom_server.manabom.application.tingPurchase.port.StorePurchaseInvalidException;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.ConsumeAttempt;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.CreditOutcome;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.VerificationAttempt;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 팅 인앱결제 검증 흐름을 조율한다.
 * 1) VERIFYING 구매 기록 생성(커밋) -> 2) 스토어 검증(외부 호출) -> 3) 지급(커밋) -> 4) Google consume
 */
@Slf4j
@Service
public class TingPurchaseService {

    private final TingPurchaseStateService stateService;
    private final Map<PurchaseStore, InAppPurchaseGateway> gateways;
    private final TingPurchaseRepository tingPurchaseRepository;
    private final StorePaymentRepository storePaymentRepository;
    private final TingWalletRepository tingWalletRepository;

    public TingPurchaseService(
            TingPurchaseStateService stateService,
            List<InAppPurchaseGateway> gateways,
            TingPurchaseRepository tingPurchaseRepository,
            StorePaymentRepository storePaymentRepository,
            TingWalletRepository tingWalletRepository
    ) {
        this.stateService = stateService;
        this.gateways = new EnumMap<>(PurchaseStore.class);
        gateways.forEach(gateway -> this.gateways.put(gateway.store(), gateway));
        this.tingPurchaseRepository = tingPurchaseRepository;
        this.storePaymentRepository = storePaymentRepository;
        this.tingWalletRepository = tingWalletRepository;
    }

    public TingPurchaseResponse verify(Long userId, VerifyTingPurchaseRequest request) {
        VerificationAttempt attempt = begin(userId, request);

        switch (attempt.status()) {
            case PAID, REFUNDED -> {
                return getPurchase(userId, attempt.paymentId());
            }
            case FAILED -> throw new IllegalStateException(
                    "스토어 검증에 실패한 구매입니다: " + attempt.failureReason()
            );
            case VERIFYING -> {
                // 아래에서 처리
            }
            default -> throw new IllegalStateException("검증할 수 없는 결제 상태입니다: " + attempt.status());
        }

        StorePurchase result;
        try {
            result = gatewayFor(attempt.store()).verify(
                    new VerifyPurchaseCommand(attempt.storeProductId(), attempt.purchaseKey())
            );
        } catch (StorePurchaseInvalidException e) {
            stateService.fail(attempt.paymentId(), safeReason(e));
            log.warn("스토어 구매 검증 거절. paymentId={}, store={}, reason={}",
                    attempt.paymentId(), attempt.store(), e.getMessage());
            throw new IllegalStateException("스토어 구매 검증에 실패했습니다: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            stateService.recordTransientFailure(attempt.paymentId(), safeReason(e));
            log.error("스토어 구매 검증 중 오류. paymentId={}, store={}", attempt.paymentId(), attempt.store(), e);
            throw e;
        }

        CreditOutcome outcome = stateService.credit(attempt.paymentId(), result);
        switch (outcome.result()) {
            case REJECTED -> throw new IllegalStateException("스토어 구매 검증에 실패했습니다: " + outcome.reason());
            case PENDING -> throw new IllegalStateException(
                    "스토어 결제가 아직 완료되지 않았습니다. 결제 완료 후 다시 시도해 주세요."
            );
            default -> {
                // CREDITED / ALREADY_CREDITED
            }
        }

        consume(attempt.paymentId());
        return getPurchase(userId, attempt.paymentId());
    }

    /**
     * Google 소모성 상품 consume. 실패해도 지급은 유효하며 스케줄러가 재시도한다.
     */
    public void consume(Long paymentId) {
        ConsumeAttempt attempt = stateService.startConsume(paymentId);
        if (attempt == null) {
            return;
        }
        try {
            gatewayFor(attempt.store()).consume(
                    new ConsumePurchaseCommand(attempt.storeProductId(), attempt.purchaseKey())
            );
            stateService.completeConsume(paymentId);
        } catch (RuntimeException e) {
            stateService.failConsume(paymentId);
            log.error("스토어 구매 consume 실패. paymentId={}, store={}", paymentId, attempt.store(), e);
        }
    }

    @Transactional(readOnly = true)
    public TingPurchaseResponse getPurchase(Long userId, Long paymentId) {
        TingPurchase purchase = tingPurchaseRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("팅 구매 내역을 찾을 수 없습니다."));
        if (userId == null || !userId.equals(purchase.getPayment().getUserId())) {
            throw new IllegalArgumentException("본인의 팅 구매 내역만 조회할 수 있습니다.");
        }
        StorePayment storePayment = storePaymentRepository.findByIdWithPayment(paymentId).orElse(null);
        TingWallet wallet = tingWalletRepository.findByUserId(userId).orElse(null);
        return TingPurchaseResponse.from(
                purchase.getPayment(),
                storePayment,
                purchase,
                wallet == null ? null : wallet.getTing(),
                wallet == null ? null : wallet.getEventTing()
        );
    }

    @Transactional(readOnly = true)
    public List<TingPurchaseResponse> getRecentPurchases(Long userId, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 정보가 필요합니다.");
        }
        int limit = Math.min(Math.max(size, 1), 100);
        return tingPurchaseRepository.findRecentByUserId(userId, PageRequest.of(0, limit))
                .stream()
                .map(purchase -> TingPurchaseResponse.from(
                        purchase.getPayment(),
                        storePaymentRepository.findByIdWithPayment(purchase.getPaymentId()).orElse(null),
                        purchase,
                        null,
                        null
                ))
                .toList();
    }

    private VerificationAttempt begin(Long userId, VerifyTingPurchaseRequest request) {
        try {
            return stateService.begin(userId, request.store(), request.productCode(), request.purchaseKey());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 구매가 등록된 경우: 다시 조회하면 기존 건이 잡힌다.
            log.info("스토어 구매 동시 등록 감지. store={}, 재조회합니다.", request.store());
            return stateService.begin(userId, request.store(), request.productCode(), request.purchaseKey());
        }
    }

    private InAppPurchaseGateway gatewayFor(PurchaseStore store) {
        InAppPurchaseGateway gateway = gateways.get(store);
        if (gateway == null) {
            throw new IllegalStateException(store.getDescription() + " 결제 게이트웨이가 설정되지 않았습니다.");
        }
        return gateway;
    }

    private String safeReason(RuntimeException exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
