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
    private final GifticonOrderCompletionService completionService;
    private final GifticonOrderRequester gifticonOrderRequester;
    private final GifticonPaymentService paymentService;
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
        } catch (Exception e) {
            String failureReason = safeFailureReason(e);
            GifticonOrderAttemptService.OrderFailure failure =
                    attemptService.markFailed(gifticonOrderId, attemptedAt, failureReason);
            log.error(
                    "[Gift Biz] 선물 발송 요청 실패. gifticonOrderId={}, externalOrderId={}",
                    gifticonOrderId,
                    attempt.externalOrderId(),
                    e
            );
            if (failure.chatPaymentId() != null && failure.attemptsExhausted()) {
                paymentService.failChatDeliveryAndRefund(
                        failure.chatPaymentId(),
                        failureReason
                );
            }
            return;
        }

        try {
            completionService.completeRequested(gifticonOrderId, attemptedAt);
            log.info(
                    "[Gift Biz] 선물 발송 요청 접수 완료. gifticonOrderId={}, externalOrderId={}",
                    gifticonOrderId,
                    attempt.externalOrderId()
            );
        } catch (RuntimeException e) {
            attemptService.markFailed(gifticonOrderId, attemptedAt, safeFailureReason(e));
            log.error(
                    "[Gift Biz] 발송 접수 후 내부 상태 반영 실패. gifticonOrderId={}, externalOrderId={}",
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
