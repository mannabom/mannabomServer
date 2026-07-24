package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.port.GifticonOrderRequester;
import mannabom_server.manabom.application.gifticon.port.GifticonTokenCipher;
import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GifticonOrderProcessor {

    private final GifticonOrderRepository gifticonOrderRepository;
    private final GifticonOrderRequester gifticonOrderRequester;
    private final GifticonTokenCipher gifticonTokenCipher;
    private final GiftbizProperties giftbizProperties;
    private final Clock clock = Clock.systemUTC();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long gifticonOrderId) {
        GifticonOrder order = gifticonOrderRepository.findByIdForUpdate(gifticonOrderId)
                .orElse(null);
        if (order == null) {
            log.warn("[Gift Biz] 발송 대기 주문을 찾을 수 없습니다. gifticonOrderId={}", gifticonOrderId);
            return;
        }

        int maxAttempts = giftbizProperties.getOrder().getRetry().getMaxAttempts();
        if (!order.canAttempt(maxAttempts)) {
            return;
        }

        Instant attemptedAt = clock.instant();
        try {
            GifticonProduct product = order.getMessageRequest().getGifticonProduct();
            if (product == null || !product.hasTemplateToken()) {
                throw new IllegalStateException("발송 가능한 템플릿 토큰이 등록되지 않은 기프티콘 상품입니다.");
            }
            gifticonOrderRequester.requestGift(new GifticonOrderCommand(
                    gifticonTokenCipher.decrypt(product.getEncryptedTemplateToken()),
                    order.getSenderNickname(),
                    order.getReceiverPhone(),
                    order.getReceiverName(),
                    order.getExternalKey(),
                    order.getExternalOrderId()
            ));
            order.markRequested(attemptedAt);
            log.info(
                    "[Gift Biz] 선물 발송 요청 접수 완료. gifticonOrderId={}, externalOrderId={}",
                    order.getGifticonOrderId(),
                    order.getExternalOrderId()
            );
        } catch (Exception e) {
            order.markFailed(attemptedAt, safeFailureReason(e));
            log.error(
                    "[Gift Biz] 선물 발송 요청 실패. gifticonOrderId={}, externalOrderId={}, attempt={}",
                    order.getGifticonOrderId(),
                    order.getExternalOrderId(),
                    order.getAttemptCount(),
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
