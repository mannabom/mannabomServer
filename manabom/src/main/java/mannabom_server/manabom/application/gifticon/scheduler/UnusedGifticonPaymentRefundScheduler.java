package mannabom_server.manabom.application.gifticon.scheduler;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.toss-payments.unused-payment-refund",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UnusedGifticonPaymentRefundScheduler {

    private final GifticonPaymentRepository paymentRepository;
    private final GifticonPaymentService paymentService;
    private final TossPaymentsProperties properties;

    @Scheduled(
            initialDelayString = "${app.toss-payments.unused-payment-refund.initial-delay:60000}",
            fixedDelayString = "${app.toss-payments.unused-payment-refund.fixed-delay:60000}"
    )
    public void refundUnusedPayments() {
        TossPaymentsProperties.UnusedPaymentRefund policy =
                properties.getUnusedPaymentRefund();
        long gracePeriodMinutes = Math.max(1L, policy.getGracePeriodMinutes());
        Instant approvedBefore = Instant.now()
                .minus(gracePeriodMinutes, ChronoUnit.MINUTES);
        List<Long> paymentIds = paymentRepository.findUnusedPaidPaymentIds(
                GifticonPaymentStatus.PAID,
                approvedBefore,
                PageRequest.of(0, Math.max(1, policy.getBatchSize()))
        );

        paymentIds.forEach(paymentId ->
                paymentService.requestExpiredUnusedPaymentRefund(
                        paymentId,
                        approvedBefore
                )
        );
    }
}
