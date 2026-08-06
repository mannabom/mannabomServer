package mannabom_server.manabom.application.gifticon.scheduler;

import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GifticonMessageCreationRetrySchedulerTest {

    @Test
    void retriesMessageCreationWithoutFrontendRequest() {
        GifticonPaymentRepository repository = mock(GifticonPaymentRepository.class);
        GifticonPaymentService paymentService = mock(GifticonPaymentService.class);
        TossPaymentsProperties properties = new TossPaymentsProperties();
        when(repository.findMessageCreationRetryIds(
                eq(GifticonPaymentStatus.PAID),
                eq(GifticonPaymentPurpose.MESSAGE_REQUEST),
                any(),
                eq(10),
                eq(GifticonMessageCreationStatus.PROCESSING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(10L, 20L));
        GifticonMessageCreationRetryScheduler scheduler =
                new GifticonMessageCreationRetryScheduler(
                        repository,
                        paymentService,
                        properties
                );

        scheduler.retry();

        verify(paymentService).createMessageForPaidPayment(10L);
        verify(paymentService).createMessageForPaidPayment(20L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<GifticonMessageCreationStatus>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(repository).findMessageCreationRetryIds(
                eq(GifticonPaymentStatus.PAID),
                eq(GifticonPaymentPurpose.MESSAGE_REQUEST),
                statuses.capture(),
                eq(10),
                eq(GifticonMessageCreationStatus.PROCESSING),
                any(Instant.class),
                any(Pageable.class)
        );
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                GifticonMessageCreationStatus.PENDING,
                GifticonMessageCreationStatus.PROCESSING,
                GifticonMessageCreationStatus.RETRY_PENDING
        );
    }
}
