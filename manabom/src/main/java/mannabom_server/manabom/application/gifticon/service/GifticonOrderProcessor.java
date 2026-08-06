package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.port.GifticonOrderRequester;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GifticonOrderProcessor {

    private final GifticonOrderAttemptService attemptService;
    private final GifticonOrderRequester gifticonOrderRequester;
    private final Clock clock = Clock.systemUTC();

    public void process(Long gifticonOrderId) {
        GifticonOrderAttemptService.GifticonOrderAttempt attempt =
                attemptService.prepare(gifticonOrderId);
        if (attempt == null) {
            return;
        }

        Instant attemptedAt = clock.instant();
        try {
            gifticonOrderRequester.requestGift(attempt.command());
            attemptService.markRequested(gifticonOrderId, attemptedAt);
            log.info(
                    "[Gift Biz] 선물 발송 요청 접수 완료. gifticonOrderId={}, externalOrderId={}",
                    gifticonOrderId,
                    attempt.externalOrderId()
            );
        } catch (Exception e) {
            attemptService.markFailed(gifticonOrderId, attemptedAt, safeFailureReason(e));
            log.error(
                    "[Gift Biz] 선물 발송 요청 실패. gifticonOrderId={}, externalOrderId={}",
                    gifticonOrderId,
                    attempt.externalOrderId(),
                    e
            );
        }
    }

    private String safeFailureReason(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
