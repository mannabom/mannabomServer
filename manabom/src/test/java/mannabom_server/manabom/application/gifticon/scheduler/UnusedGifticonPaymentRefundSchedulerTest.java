package mannabom_server.manabom.application.gifticon.scheduler;

import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnusedGifticonPaymentRefundSchedulerTest {

    @Mock
    private GifticonPaymentRepository paymentRepository;
    @Mock
    private GifticonPaymentService paymentService;

    @Test
    void requestsRefundForPaidPaymentsUnusedPastGracePeriod() {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.getUnusedPaymentRefund().setGracePeriodMinutes(30L);
        properties.getUnusedPaymentRefund().setBatchSize(20);
        when(paymentRepository.findUnusedPaidPaymentIds(
                eq(GifticonPaymentStatus.PAID),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(20L));
        UnusedGifticonPaymentRefundScheduler scheduler =
                new UnusedGifticonPaymentRefundScheduler(
                        paymentRepository,
                        paymentService,
                        properties
                );

        scheduler.refundUnusedPayments();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(paymentService).requestExpiredUnusedPaymentRefund(
                eq(20L),
                cutoffCaptor.capture()
        );
        assertThat(cutoffCaptor.getValue())
                .isBeforeOrEqualTo(Instant.now().minusSeconds(30L * 60L));
    }
}
