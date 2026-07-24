package mannabom_server.manabom.application.gifticon.scheduler;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.service.GifticonOrderProcessor;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.kakao.giftbiz.order.retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GifticonOrderRetryScheduler {

    private static final List<GifticonOrderStatus> RETRYABLE_STATUSES =
            List.of(GifticonOrderStatus.PENDING, GifticonOrderStatus.FAILED);

    private final GifticonOrderRepository gifticonOrderRepository;
    private final GifticonOrderProcessor gifticonOrderProcessor;
    private final GiftbizProperties giftbizProperties;

    @Scheduled(
            initialDelayString = "${app.kakao.giftbiz.order.retry.initial-delay:60000}",
            fixedDelayString = "${app.kakao.giftbiz.order.retry.fixed-delay:60000}"
    )
    public void retry() {
        GiftbizProperties.Retry retry = giftbizProperties.getOrder().getRetry();
        List<Long> orderIds = gifticonOrderRepository.findRetryableOrderIds(
                RETRYABLE_STATUSES,
                retry.getMaxAttempts(),
                PageRequest.of(0, retry.getBatchSize())
        );
        orderIds.forEach(gifticonOrderProcessor::process);
    }
}
