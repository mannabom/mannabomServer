package mannabom_server.manabom.application.tingPurchase.scheduler;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseService;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.policy.config.TingPurchaseProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Google Play consume 이 실패한 구매를 주기적으로 재시도한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ting-purchase.consume-retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class StorePaymentConsumeRetryScheduler {

    private static final List<StoreConsumeStatus> RETRYABLE_STATUSES = List.of(
            StoreConsumeStatus.PENDING,
            StoreConsumeStatus.PROCESSING,
            StoreConsumeStatus.FAILED
    );

    private final StorePaymentRepository storePaymentRepository;
    private final TingPurchaseService purchaseService;
    private final TingPurchaseStateService stateService;
    private final TingPurchaseProperties properties;

    @Scheduled(
            initialDelayString = "${app.ting-purchase.consume-retry.initial-delay:60000}",
            fixedDelayString = "${app.ting-purchase.consume-retry.fixed-delay:60000}"
    )
    public void retry() {
        TingPurchaseProperties.ConsumeRetry retry = properties.getConsumeRetry();
        List<Long> paymentIds = storePaymentRepository.findConsumeRetryIds(
                PaymentStatus.PAID,
                RETRYABLE_STATUSES,
                Math.max(1, retry.getMaxAttempts()),
                StoreConsumeStatus.PROCESSING,
                Instant.now().minusSeconds(stateService.staleSeconds()),
                PageRequest.of(0, Math.max(1, retry.getBatchSize()))
        );
        paymentIds.forEach(purchaseService::consume);
    }
}
