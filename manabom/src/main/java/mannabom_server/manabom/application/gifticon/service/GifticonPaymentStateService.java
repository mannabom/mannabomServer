package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GifticonPaymentStateService {

    private final GifticonPaymentRepository paymentRepository;
    private final TossPaymentsProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationAttempt startConfirmation(
            Long userId,
            String orderId,
            String paymentKey,
            int amount
    ) {
        GifticonPayment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제 주문을 찾을 수 없습니다."));
        validateOwner(payment, userId);
        if (payment.getAmount() != amount) {
            throw new IllegalArgumentException("결제 금액이 서버 주문 금액과 일치하지 않습니다.");
        }
        if (payment.getStatus() == GifticonPaymentStatus.PAID) {
            if (!paymentKey.equals(payment.getPaymentKey())) {
                throw new IllegalStateException("이미 다른 결제 키로 승인된 주문입니다.");
            }
            return ConfirmationAttempt.alreadyPaid(payment);
        }

        Instant now = Instant.now();
        payment.startConfirmation(
                paymentKey,
                now,
                now.minusSeconds(Math.max(60L, properties.getRequestTimeoutSeconds() * 3L))
        );
        return ConfirmationAttempt.pending(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GifticonPayment completeConfirmation(Long paymentId) {
        GifticonPayment payment = findForUpdate(paymentId);
        payment.markPaid(Instant.now());
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failConfirmation(Long paymentId, String reason) {
        findForUpdate(paymentId).markConfirmationFailed(reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startMessageCreation(Long paymentId) {
        GifticonPayment payment = findForUpdate(paymentId);
        int maxAttempts = Math.max(
                1,
                properties.getMessageCreationRetry().getMaxAttempts()
        );
        Instant now = Instant.now();
        if (!payment.canStartMessageCreation(
                maxAttempts,
                now.minusSeconds(Math.max(
                        60L,
                        properties.getRequestTimeoutSeconds() * 3L
                ))
        )) {
            return false;
        }
        payment.startMessageCreation(now);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startMessageCreationForAdmin(Long paymentId) {
        GifticonPayment payment = findForUpdate(paymentId);
        Instant now = Instant.now();
        if (payment.getPurpose() != GifticonPaymentPurpose.MESSAGE_REQUEST
                || payment.getStatus() != GifticonPaymentStatus.PAID
                || payment.getMessageRequest() != null) {
            throw new IllegalStateException("메시지가 없는 결제 완료 건만 재시도할 수 있습니다.");
        }
        if (payment.getMessageCreationStatus() == GifticonMessageCreationStatus.PROCESSING
                && payment.getLastMessageCreationAttemptAt() != null
                && !payment.getLastMessageCreationAttemptAt().isBefore(
                        now.minusSeconds(Math.max(
                                60L,
                                properties.getRequestTimeoutSeconds() * 3L
                        ))
        )) {
            throw new IllegalStateException("메시지 생성 처리가 진행 중입니다.");
        }
        payment.startMessageCreation(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GifticonMessageCreationStatus failMessageCreation(
            Long paymentId,
            String reason,
            boolean retryable
    ) {
        GifticonPayment payment = findForUpdate(paymentId);
        payment.markMessageCreationFailed(
                reason,
                retryable,
                Math.max(1, properties.getMessageCreationRetry().getMaxAttempts())
        );
        if (payment.getMessageCreationStatus() == GifticonMessageCreationStatus.FAILED) {
            payment.requestRefund();
        }
        return payment.getMessageCreationStatus();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failChatDelivery(Long paymentId, String reason) {
        GifticonPayment payment = findForUpdate(paymentId);
        payment.markChatDeliveryFailed(reason);
        payment.requestRefund();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundAttempt startRefund(Long paymentId) {
        GifticonPayment payment = findForUpdate(paymentId);
        Instant now = Instant.now();
        int maxAttempts = properties.getRefundRetry().getMaxAttempts();
        if (!payment.canStartRefund(
                maxAttempts,
                now.minusSeconds(Math.max(60L, properties.getRequestTimeoutSeconds() * 3L))
        )) {
            return null;
        }
        payment.startRefund(now);
        return new RefundAttempt(
                payment.getGifticonPaymentId(),
                payment.getPaymentKey(),
                payment.getOrderId(),
                payment.getMessageRequest() == null && payment.getChatMessage() == null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundAttempt startRefundForAdmin(Long paymentId) {
        GifticonPayment payment = findForUpdate(paymentId);
        if (payment.getStatus() == GifticonPaymentStatus.PAID) {
            if (payment.getMessageRequest() != null || payment.getChatMessage() != null) {
                throw new IllegalStateException("수신자에게 공개된 결제는 강제 환불할 수 없습니다.");
            }
            if (payment.getGifticonOrder() != null
                    && payment.getGifticonOrder().getStatus() != GifticonOrderStatus.FAILED) {
                throw new IllegalStateException("기프티콘 발송 처리가 진행 중인 결제는 강제 환불할 수 없습니다.");
            }
            payment.requestRefund();
        }

        Instant now = Instant.now();
        if (!payment.canStartRefund(
                Integer.MAX_VALUE,
                now.minusSeconds(Math.max(
                        60L,
                        properties.getRequestTimeoutSeconds() * 3L
                ))
        )) {
            throw new IllegalStateException("현재 상태에서는 강제 환불을 시작할 수 없습니다.");
        }
        payment.startRefund(now);
        return new RefundAttempt(
                payment.getGifticonPaymentId(),
                payment.getPaymentKey(),
                payment.getOrderId(),
                payment.getMessageRequest() == null && payment.getChatMessage() == null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRefund(Long paymentId) {
        findForUpdate(paymentId).markRefunded(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failRefund(Long paymentId, String reason) {
        findForUpdate(paymentId).markRefundFailed(reason);
    }

    private GifticonPayment findForUpdate(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
    }

    private void validateOwner(GifticonPayment payment, Long userId) {
        if (userId == null || !userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("본인의 기프티콘 결제만 처리할 수 있습니다.");
        }
    }

    public record ConfirmationAttempt(
            Long paymentId,
            String paymentKey,
            String orderId,
            int amount,
            boolean alreadyPaid
    ) {
        static ConfirmationAttempt pending(GifticonPayment payment) {
            return from(payment, false);
        }

        static ConfirmationAttempt alreadyPaid(GifticonPayment payment) {
            return from(payment, true);
        }

        private static ConfirmationAttempt from(
                GifticonPayment payment,
                boolean alreadyPaid
        ) {
            return new ConfirmationAttempt(
                    payment.getGifticonPaymentId(),
                    payment.getPaymentKey(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    alreadyPaid
            );
        }
    }

    public record RefundAttempt(
            Long paymentId,
            String paymentKey,
            String orderId,
            boolean unusedPayment
    ) {
    }
}
