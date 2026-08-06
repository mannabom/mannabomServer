package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.port.GifticonTokenCipher;
import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GifticonOrderAttemptService {

    private final GifticonOrderRepository gifticonOrderRepository;
    private final GifticonTokenCipher gifticonTokenCipher;
    private final GiftbizProperties giftbizProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GifticonOrderAttempt prepare(Long gifticonOrderId) {
        GifticonOrder order = gifticonOrderRepository.findByIdForUpdate(gifticonOrderId)
                .orElse(null);
        if (order == null) {
            log.warn("[Gift Biz] 발송 대기 주문을 찾을 수 없습니다. gifticonOrderId={}", gifticonOrderId);
            return null;
        }

        int timeoutSeconds = giftbizProperties.getRequestTimeoutSeconds();
        Instant now = Instant.now();
        if (!order.canStartAttempt(
                giftbizProperties.getOrder().getRetry().getMaxAttempts(),
                now.minusSeconds(Math.max(60L, timeoutSeconds * 3L))
        )) {
            return null;
        }

        order.markProcessing(now);
        try {
            GifticonProduct product = order.getPayment() == null
                    ? order.getMessageRequest().getGifticonProduct()
                    : order.getPayment().getProduct();
            if (product == null || !product.hasTemplateToken()) {
                throw new IllegalStateException("발송 가능한 템플릿 토큰이 등록되지 않은 기프티콘 상품입니다.");
            }
            return new GifticonOrderAttempt(
                    new GifticonOrderCommand(
                            gifticonTokenCipher.decrypt(product.getEncryptedTemplateToken()),
                            order.getSenderNickname(),
                            order.getReceiverPhone(),
                            order.getReceiverName(),
                            order.getExternalKey(),
                            order.getExternalOrderId()
                    ),
                    order.getExternalOrderId()
            );
        } catch (RuntimeException e) {
            order.markFailed(now, safeFailureReason(e));
            log.error(
                    "[Gift Biz] 발송 데이터 준비 실패. gifticonOrderId={}",
                    gifticonOrderId,
                    e
            );
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRequested(Long gifticonOrderId, Instant requestedAt) {
        GifticonOrder order = gifticonOrderRepository.findByIdForUpdate(gifticonOrderId)
                .orElseThrow(() -> new IllegalStateException("기프티콘 주문을 찾을 수 없습니다."));
        if (order.getStatus() == GifticonOrderStatus.PROCESSING) {
            order.markRequested(requestedAt);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderFailure markFailed(Long gifticonOrderId, Instant failedAt, String reason) {
        GifticonOrder order = gifticonOrderRepository.findByIdForUpdate(gifticonOrderId)
                .orElseThrow(() -> new IllegalStateException("기프티콘 주문을 찾을 수 없습니다."));
        if (order.getStatus() == GifticonOrderStatus.PROCESSING) {
            order.markFailed(failedAt, reason);
        }
        boolean attemptsExhausted = order.hasExhaustedAttempts(
                giftbizProperties.getOrder().getRetry().getMaxAttempts()
        );
        Long chatPaymentId = order.getPayment() != null
                && order.getPayment().getPurpose()
                == GifticonPaymentPurpose.CHAT
                ? order.getPayment().getGifticonPaymentId()
                : null;
        return new OrderFailure(chatPaymentId, attemptsExhausted);
    }

    public record GifticonOrderAttempt(
            GifticonOrderCommand command,
            String externalOrderId
    ) {
    }

    public record OrderFailure(
            Long chatPaymentId,
            boolean attemptsExhausted
    ) {
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
