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

@ExtendWith(MockitoExtension.class)
class GifticonOrderProcessorTest {

    @Mock
    private GifticonOrderAttemptService attemptService;

    @Mock
    private GifticonOrderRequester gifticonOrderRequester;

    private GifticonOrderProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GifticonOrderProcessor(attemptService, gifticonOrderRequester);
    }

    @Test
    void performsRemoteRequestAfterShortPreparationTransaction() {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt = attempt();
        when(attemptService.prepare(1L)).thenReturn(attempt);

        processor.process(1L);

        verify(gifticonOrderRequester).requestGift(attempt.command());
        verify(attemptService).markRequested(org.mockito.ArgumentMatchers.eq(1L), any(Instant.class));
    }

    @Test
    void recordsFailureInSeparateTransactionWhenRemoteRequestFails() {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt = attempt();
        when(attemptService.prepare(1L)).thenReturn(attempt);
        doThrow(new IllegalStateException("temporary failure"))
                .when(gifticonOrderRequester)
                .requestGift(attempt.command());

        processor.process(1L);

        verify(attemptService).markFailed(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Instant.class),
                contains("temporary failure")
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
