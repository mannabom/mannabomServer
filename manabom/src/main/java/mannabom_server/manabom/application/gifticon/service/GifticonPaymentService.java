package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.dto.request.ConfirmGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.request.PrepareGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentPrepareResponse;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentResponse;
import mannabom_server.manabom.application.gifticon.event.GifticonPaymentRefundRequestedEvent;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.CancelPaymentCommand;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.ConfirmPaymentCommand;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.PaymentResult;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import mannabom_server.manabom.application.messageRequest.service.MessageRequestService;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GifticonPaymentService {

    private static final String CONFIRM_STATUS = "DONE";
    private static final String CANCEL_STATUS = "CANCELED";
    private static final String REJECTED_MESSAGE_REFUND_REASON = "메시지 요청 거절";
    private static final String UNUSED_PAYMENT_REFUND_REASON = "미사용 기프티콘 결제 취소";

    private final GifticonPaymentRepository paymentRepository;
    private final GifticonProductRepository productRepository;
    private final GifticonPaymentStateService stateService;
    private final GifticonPaymentGateway paymentGateway;
    private final TossPaymentsProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageRequestService messageRequestService;

    @Transactional
    public GifticonPaymentPrepareResponse prepare(
            Long userId,
            PrepareGifticonPaymentRequest request
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("결제 사용자 정보가 필요합니다.");
        }
        if (!StringUtils.hasText(properties.getClientKey())) {
            throw new IllegalStateException("TOSS_PAYMENTS_CLIENT_KEY 설정이 필요합니다.");
        }

        messageRequestService.validateMessageIntent(
                userId,
                request.targetProfileId(),
                request.message(),
                request.source()
        );

        GifticonProduct product = productRepository.findById(request.gifticonProductId())
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 상품을 찾을 수 없습니다."));
        if (!product.isOrderableAt(LocalDateTime.now())) {
            throw new IllegalStateException("현재 결제할 수 없는 기프티콘 상품입니다.");
        }

        GifticonPayment payment = paymentRepository.save(new GifticonPayment(
                userId,
                product,
                createOrderId(),
                createCustomerKey(),
                request.targetProfileId(),
                request.message(),
                request.source()
        ));
        return new GifticonPaymentPrepareResponse(
                payment.getGifticonPaymentId(),
                payment.getOrderId(),
                payment.getCustomerKey(),
                orderName(product),
                payment.getAmount(),
                properties.getClientKey().trim()
        );
    }

    public GifticonPaymentResponse confirm(
            Long userId,
            ConfirmGifticonPaymentRequest request
    ) {
        GifticonPaymentStateService.ConfirmationAttempt attempt =
                stateService.startConfirmation(
                        userId,
                        request.orderId(),
                        request.paymentKey(),
                        request.amount()
                );
        if (!attempt.alreadyPaid()) {
            try {
                PaymentResult result = paymentGateway.confirm(new ConfirmPaymentCommand(
                        attempt.paymentKey(),
                        attempt.orderId(),
                        attempt.amount(),
                        "GIFTICON_CONFIRM_" + attempt.paymentId()
                ));
                validateConfirmation(attempt, result);
                stateService.completeConfirmation(attempt.paymentId());
            } catch (RuntimeException e) {
                stateService.failConfirmation(attempt.paymentId(), safeFailureReason(e));
                throw e;
            }
        }

        createMessageForPaidPayment(attempt.paymentId());
        return getPayment(userId, attempt.paymentId());
    }

    public void createMessageForPaidPayment(Long paymentId) {
        if (!stateService.startMessageCreation(paymentId)) {
            return;
        }

        createMessageAfterStarted(paymentId);
    }

    public void retryMessageForAdmin(Long paymentId) {
        stateService.startMessageCreationForAdmin(paymentId);
        createMessageAfterStarted(paymentId);
    }

    private void createMessageAfterStarted(Long paymentId) {
        try {
            messageRequestService.sendPaidGifticonMessage(paymentId);
        } catch (RuntimeException e) {
            GifticonMessageCreationStatus messageStatus =
                    stateService.failMessageCreation(
                            paymentId,
                            safeFailureReason(e),
                            isRetryableMessageCreationFailure(e)
                    );
            log.error(
                    "결제 완료 후 기프티콘 메시지 생성 실패. paymentId={}, messageStatus={}",
                    paymentId,
                    messageStatus,
                    e
            );
            if (messageStatus == GifticonMessageCreationStatus.FAILED) {
                refund(paymentId);
            }
        }
    }

    @Transactional(readOnly = true)
    public GifticonPaymentResponse getPayment(Long userId, Long paymentId) {
        GifticonPayment payment = paymentRepository.findByIdWithMessageRequest(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
        if (userId == null || !userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("본인의 기프티콘 결제만 조회할 수 있습니다.");
        }
        return GifticonPaymentResponse.from(payment);
    }

    @Transactional
    public GifticonPaymentResponse cancelUnusedPayment(Long userId, Long paymentId) {
        GifticonPayment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
        validateOwner(payment, userId);
        if (payment.getMessageRequest() != null) {
            throw new IllegalStateException("이미 메시지 요청에 사용된 기프티콘 결제입니다.");
        }

        requestRefundIfPaid(payment);
        return GifticonPaymentResponse.from(payment);
    }

    @Transactional
    public void requestExpiredUnusedPaymentRefund(
            Long paymentId,
            Instant approvedBefore
    ) {
        GifticonPayment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElse(null);
        if (payment == null
                || payment.getMessageRequest() != null
                || payment.getStatus() != GifticonPaymentStatus.PAID
                || payment.getApprovedAt() == null
                || payment.getApprovedAt().isAfter(approvedBefore)) {
            return;
        }

        requestRefundIfPaid(payment);
    }

    public void refund(Long paymentId) {
        executeRefund(stateService.startRefund(paymentId));
    }

    public void refundForAdmin(Long paymentId) {
        executeRefund(stateService.startRefundForAdmin(paymentId));
    }

    private void executeRefund(GifticonPaymentStateService.RefundAttempt attempt) {
        if (attempt == null) {
            return;
        }

        try {
            PaymentResult result = paymentGateway.cancel(new CancelPaymentCommand(
                    attempt.paymentKey(),
                    attempt.unusedPayment()
                            ? UNUSED_PAYMENT_REFUND_REASON
                            : REJECTED_MESSAGE_REFUND_REASON,
                    "GIFTICON_REFUND_" + attempt.paymentId()
            ));
            if (!CANCEL_STATUS.equals(result.status())) {
                throw new IllegalStateException(
                        "토스 결제가 전액 취소 상태가 아닙니다: " + result.status()
                );
            }
            stateService.completeRefund(attempt.paymentId());
        } catch (RuntimeException e) {
            stateService.failRefund(attempt.paymentId(), safeFailureReason(e));
            log.error(
                    "토스 기프티콘 결제 환불 실패. paymentId={}, orderId={}",
                    attempt.paymentId(),
                    attempt.orderId(),
                    e
            );
        }
    }

    private void validateConfirmation(
            GifticonPaymentStateService.ConfirmationAttempt attempt,
            PaymentResult result
    ) {
        if (!attempt.paymentKey().equals(result.paymentKey())
                || !attempt.orderId().equals(result.orderId())
                || attempt.amount() != result.totalAmount()
                || !CONFIRM_STATUS.equals(result.status())) {
            throw new IllegalStateException("토스 결제 승인 결과가 서버 주문 정보와 일치하지 않습니다.");
        }
    }

    private void requestRefundIfPaid(GifticonPayment payment) {
        if (payment.getStatus() == GifticonPaymentStatus.PAID) {
            payment.requestRefund();
            eventPublisher.publishEvent(
                    new GifticonPaymentRefundRequestedEvent(payment.getGifticonPaymentId())
            );
            return;
        }
        if (payment.getStatus() != GifticonPaymentStatus.REFUND_PENDING
                && payment.getStatus() != GifticonPaymentStatus.REFUND_PROCESSING
                && payment.getStatus() != GifticonPaymentStatus.REFUND_FAILED
                && payment.getStatus() != GifticonPaymentStatus.REFUNDED) {
            throw new IllegalStateException("결제가 완료된 미사용 기프티콘만 취소할 수 있습니다.");
        }
    }

    private void validateOwner(GifticonPayment payment, Long userId) {
        if (userId == null || !userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("본인의 기프티콘 결제만 취소할 수 있습니다.");
        }
    }

    private String createOrderId() {
        return "GIFTICON_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String createCustomerKey() {
        return "CUSTOMER_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String orderName(GifticonProduct product) {
        String name = product.getProductName() + " 기프티콘";
        return name.length() <= 100 ? name : name.substring(0, 100);
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private boolean isRetryableMessageCreationFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof TransientDataAccessException
                    || current instanceof CannotCreateTransactionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
