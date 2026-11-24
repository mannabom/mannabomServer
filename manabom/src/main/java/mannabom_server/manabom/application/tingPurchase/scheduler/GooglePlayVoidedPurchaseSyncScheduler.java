package mannabom_server.manabom.application.tingPurchase.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.port.GooglePlayVoidedPurchaseSource;
import mannabom_server.manabom.application.tingPurchase.port.GooglePlayVoidedPurchaseSource.VoidedPurchase;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseRefundService;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseRefundService.RefundOutcome;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.policy.config.TingPurchaseProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Google Play 환불/취소 구매(voided purchases)를 주기적으로 조회해 팅을 회수한다.
 * Google 은 환불을 서버로 푸시하지 않으므로(RTDN 은 별도 Pub/Sub 설정 필요) 폴링이 기본 경로다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ting-purchase.voided-purchase-sync",
        name = "enabled",
        havingValue = "true"
)
public class GooglePlayVoidedPurchaseSyncScheduler {

    private final GooglePlayVoidedPurchaseSource voidedPurchaseSource;
    private final TingPurchaseRefundService refundService;
    private final TingPurchaseProperties properties;

    @Scheduled(
            initialDelayString = "${app.ting-purchase.voided-purchase-sync.initial-delay:120000}",
            fixedDelayString = "${app.ting-purchase.voided-purchase-sync.fixed-delay:3600000}"
    )
    public void sync() {
        int lookbackDays = Math.min(Math.max(properties.getVoidedPurchaseSync().getLookbackDays(), 1), 30);
        Instant since = Instant.now().minus(Duration.ofDays(lookbackDays));
        List<VoidedPurchase> voided;
        try {
            voided = voidedPurchaseSource.listVoidedPurchases(since);
        } catch (RuntimeException e) {
            log.error("Google Play voided purchases 조회 실패", e);
            return;
        }
        int applied = 0;
        for (VoidedPurchase purchase : voided) {
            try {
                RefundOutcome outcome = refundService.refundByPurchaseKey(
                        PurchaseStore.GOOGLE,
                        purchase.purchaseToken(),
                        purchase.reason(),
                        purchase.voidedAt()
                );
                if (outcome == RefundOutcome.REFUNDED
                        || outcome == RefundOutcome.REFUNDED_WITH_SHORTFALL
                        || outcome == RefundOutcome.FAILED_BEFORE_CREDIT) {
                    applied++;
                    log.info("Google 환불 반영. orderId={}, outcome={}", purchase.orderId(), outcome);
                }
            } catch (RuntimeException e) {
                log.error("Google 환불 반영 실패. orderId={}", purchase.orderId(), e);
            }
        }
        if (!voided.isEmpty()) {
            log.info("Google voided purchases 동기화 완료. 조회={}, 신규 반영={}", voided.size(), applied);
        }
    }
}
