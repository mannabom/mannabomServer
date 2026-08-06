package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.application.gifticon.port.GifticonOrderRequester;
import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GifticonOrderProcessorTest {

    @Mock
    private GifticonOrderAttemptService attemptService;

    @Mock
    private GifticonOrderRequester gifticonOrderRequester;
    @Mock
    private GifticonOrderCompletionService completionService;
    @Mock
    private GifticonPaymentService paymentService;

    private GifticonOrderProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GifticonOrderProcessor(
                attemptService,
                completionService,
                gifticonOrderRequester,
                paymentService
        );
    }

    @Test
    void performsRemoteRequestAfterShortPreparationTransaction() {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt = attempt();
        when(attemptService.prepare(1L)).thenReturn(attempt);

        processor.process(1L);

        verify(gifticonOrderRequester).requestGift(attempt.command());
        verify(completionService).completeRequested(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Instant.class)
        );
    }

    @Test
    void recordsFailureInSeparateTransactionWhenRemoteRequestFails() {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt = attempt();
        when(attemptService.prepare(1L)).thenReturn(attempt);
        doThrow(new IllegalStateException("temporary failure"))
                .when(gifticonOrderRequester)
                .requestGift(attempt.command());
        when(attemptService.markFailed(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Instant.class),
                contains("temporary failure")
        )).thenReturn(new GifticonOrderAttemptService.OrderFailure(null, false));

        processor.process(1L);

        verify(attemptService).markFailed(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Instant.class),
                contains("temporary failure")
        );
        verifyNoInteractions(paymentService);
    }

    @Test
    void refundsChatPaymentOnlyAfterFinalExternalDeliveryFailure() {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt = attempt();
        when(attemptService.prepare(1L)).thenReturn(attempt);
        doThrow(new IllegalStateException("permanent failure"))
                .when(gifticonOrderRequester)
                .requestGift(attempt.command());
        when(attemptService.markFailed(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Instant.class),
                contains("permanent failure")
        )).thenReturn(new GifticonOrderAttemptService.OrderFailure(55L, true));

        processor.process(1L);

        verify(paymentService).failChatDeliveryAndRefund(
                org.mockito.ArgumentMatchers.eq(55L),
                contains("permanent failure")
        );
        verify(completionService, never()).completeRequested(
                org.mockito.ArgumentMatchers.anyLong(),
                any(Instant.class)
        );
    }

    private GifticonOrderAttemptService.GifticonOrderAttempt attempt() {
        GifticonOrderCommand command = new GifticonOrderCommand(
                "plain-token",
                "보낸사람",
                "01012345678",
                "수신자",
                "MESSAGE-GIFT-1-2",
                "MESSAGE-GIFT-1"
        );
        return new GifticonOrderAttemptService.GifticonOrderAttempt(
                command,
                "MESSAGE-GIFT-1"
        );
    }
}
