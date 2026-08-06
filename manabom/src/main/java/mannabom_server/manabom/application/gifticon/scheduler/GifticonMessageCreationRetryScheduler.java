package mannabom_server.manabom.application.gifticon.scheduler;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
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
        prefix = "app.toss-payments.message-creation-retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GifticonMessageCreationRetryScheduler {

    private static final List<GifticonMessageCreationStatus> RETRYABLE_STATUSES = List.of(
            GifticonMessageCreationStatus.PENDING,
            GifticonMessageCreationStatus.PROCESSING,
            GifticonMessageCreationStatus.RETRY_PENDING
    );

    private final GifticonPaymentRepository paymentRepository;
    private final GifticonPaymentService paymentService;
    private final TossPaymentsProperties properties;

    @Scheduled(
            initialDelayString = "${app.toss-payments.message-creation-retry.initial-delay:60000}",
            fixedDelayString = "${app.toss-payments.message-creation-retry.fixed-delay:60000}"
    )
    public void retry() {
        TossPaymentsProperties.MessageCreationRetry retry =
                properties.getMessageCreationRetry();
        List<Long> paymentIds = paymentRepository.findMessageCreationRetryIds(
                GifticonPaymentStatus.PAID,
                GifticonPaymentPurpose.MESSAGE_REQUEST,
                RETRYABLE_STATUSES,
                retry.getMaxAttempts(),
                GifticonMessageCreationStatus.PROCESSING,
                Instant.now().minusSeconds(Math.max(
                        60L,
                        properties.getRequestTimeoutSeconds() * 3L
                )),
                PageRequest.of(0, Math.max(1, retry.getBatchSize()))
        );
        paymentIds.forEach(paymentService::createMessageForPaidPayment);
    }
}
