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
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.toss-payments.refund-retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GifticonPaymentRefundRetryScheduler {

    private static final List<GifticonPaymentStatus> RETRYABLE_STATUSES = List.of(
            GifticonPaymentStatus.REFUND_PENDING,
            GifticonPaymentStatus.REFUND_PROCESSING,
            GifticonPaymentStatus.REFUND_FAILED
    );

    private final GifticonPaymentRepository paymentRepository;
    private final GifticonPaymentService paymentService;
    private final TossPaymentsProperties properties;

    @Scheduled(
            initialDelayString = "${app.toss-payments.refund-retry.initial-delay:60000}",
            fixedDelayString = "${app.toss-payments.refund-retry.fixed-delay:60000}"
    )
    public void retry() {
        TossPaymentsProperties.RefundRetry retry = properties.getRefundRetry();
        List<Long> paymentIds = paymentRepository.findRefundRetryIds(
                RETRYABLE_STATUSES,
                retry.getMaxAttempts(),
                GifticonPaymentStatus.REFUND_PROCESSING,
                Instant.now().minusSeconds(Math.max(
                        60L,
                        properties.getRequestTimeoutSeconds() * 3L
                )),
                PageRequest.of(0, retry.getBatchSize())
        );
        paymentIds.forEach(paymentService::refund);
    }
}
