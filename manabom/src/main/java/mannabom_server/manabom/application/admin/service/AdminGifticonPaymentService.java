package mannabom_server.manabom.application.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonPaymentPageResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonPaymentResponse;
import mannabom_server.manabom.application.admin.enums.GifticonPaymentAttentionReason;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.PaymentLookup;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.PaymentResult;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminGifticonPaymentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> UNAPPROVED_TOSS_STATUSES = Set.of(
            "READY",
            "IN_PROGRESS",
            "WAITING_FOR_DEPOSIT"
    );

    private final GifticonPaymentRepository paymentRepository;
    private final GifticonPaymentService paymentService;
    private final GifticonPaymentGateway paymentGateway;
    private final TossPaymentsProperties properties;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AdminGifticonPaymentPageResponse getPayments(
            AdminPrincipal admin,
            int page,
            int size,
            GifticonPaymentStatus paymentStatus,
            GifticonMessageCreationStatus messageCreationStatus,
            MessageRequestStatus messageRequestStatus,
            Long userId,
            String orderId,
            boolean attentionOnly
    ) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE, AdminRole.SUPPORT);
        validatePage(page, size);
        AttentionThresholds thresholds = thresholds();
        Page<GifticonPayment> payments = paymentRepository.findForAdmin(
                paymentStatus,
                messageCreationStatus,
                messageRequestStatus,
                userId,
                StringUtils.hasText(orderId) ? orderId.trim() : "",
                attentionOnly,
                GifticonPaymentStatus.REFUND_FAILED,
                GifticonPaymentStatus.REFUND_PROCESSING,
                GifticonPaymentStatus.PAID,
                GifticonMessageCreationStatus.FAILED,
                thresholds.refundStaleBefore(),
                thresholds.messageStaleBefore(),
                thresholds.maxRefundAttempts(),
                PageRequest.of(page, size)
        );
        return new AdminGifticonPaymentPageResponse(
                payments.getContent().stream()
                        .map(payment -> toResponse(payment, thresholds, null, null, null))
                        .toList(),
                payments.getTotalElements(),
                payments.getTotalPages(),
                payments.getNumber(),
                payments.getSize()
        );
    }

    public AdminGifticonPaymentResponse getPayment(
            AdminPrincipal admin,
            Long paymentId
    ) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE, AdminRole.SUPPORT);
        return verifiedResponse(paymentId);
    }

    private AdminGifticonPaymentResponse verifiedResponse(Long paymentId) {
        GifticonPayment payment = findPayment(paymentId);
        PaymentResult tossResult = null;
        String verificationError = null;
        try {
            tossResult = paymentGateway.getPayment(new PaymentLookup(
                    payment.getPaymentKey(),
                    payment.getOrderId()
            ));
        } catch (RuntimeException e) {
            verificationError = safeError(e);
        }
        Boolean mismatch = tossResult == null
                ? null
                : isTossStatusMismatch(payment.getStatus(), tossResult.status());
        return toResponse(
                payment,
                thresholds(),
                tossResult == null ? null : tossResult.status(),
                mismatch,
                verificationError
        );
    }

    public AdminGifticonPaymentResponse retryMessage(
            AdminPrincipal admin,
            Long paymentId,
            String reason,
            String ipAddress
    ) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.OPERATOR);
        validateReason(reason);
        GifticonPayment beforePayment = findPayment(paymentId);
        if (beforePayment.getStatus() != GifticonPaymentStatus.PAID
                || beforePayment.getMessageRequest() != null) {
            throw new IllegalStateException("메시지가 없는 결제 완료 건만 재시도할 수 있습니다.");
        }
        AdminGifticonPaymentResponse before = localResponse(beforePayment);

        try {
            paymentService.retryMessageForAdmin(paymentId);
            auditPaymentAction(
                    admin,
                    AdminAuditActionType.GIFTICON_PAYMENT_MESSAGE_RETRY,
                    paymentId,
                    before,
                    reason,
                    ipAddress,
                    null
            );
            return verifiedResponse(paymentId);
        } catch (RuntimeException e) {
            auditPaymentAction(
                    admin,
                    AdminAuditActionType.GIFTICON_PAYMENT_MESSAGE_RETRY,
                    paymentId,
                    before,
                    reason,
                    ipAddress,
                    e
            );
            throw e;
        }
    }

    public AdminGifticonPaymentResponse forceRefund(
            AdminPrincipal admin,
            Long paymentId,
            String reason,
            String ipAddress
    ) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE);
        validateReason(reason);
        GifticonPayment beforePayment = findPayment(paymentId);
        AdminGifticonPaymentResponse before = localResponse(beforePayment);

        try {
            paymentService.refundForAdmin(paymentId);
            auditPaymentAction(
                    admin,
                    AdminAuditActionType.GIFTICON_PAYMENT_FORCE_REFUND,
                    paymentId,
                    before,
                    reason,
                    ipAddress,
                    null
            );
            return verifiedResponse(paymentId);
        } catch (RuntimeException e) {
            auditPaymentAction(
                    admin,
                    AdminAuditActionType.GIFTICON_PAYMENT_FORCE_REFUND,
                    paymentId,
                    before,
                    reason,
                    ipAddress,
                    e
            );
            throw e;
        }
    }

    private AdminGifticonPaymentResponse localResponse(GifticonPayment payment) {
        return toResponse(payment, thresholds(), null, null, null);
    }

    private AdminGifticonPaymentResponse toResponse(
            GifticonPayment payment,
            AttentionThresholds thresholds,
            String tossStatus,
            Boolean tossStatusMismatch,
            String tossVerificationError
    ) {
        List<AdminGifticonPaymentResponse.AttentionReason> attentionReasons =
                attentionReasons(payment, thresholds);
        if (Boolean.TRUE.equals(tossStatusMismatch)) {
            attentionReasons.add(attentionReason(
                    GifticonPaymentAttentionReason.TOSS_STATUS_MISMATCH
            ));
        }
        return new AdminGifticonPaymentResponse(
                payment.getGifticonPaymentId(),
                payment.getOrderId(),
                mask(payment.getPaymentKey()),
                payment.getUserId(),
                payment.getTargetProfileId(),
                payment.getProduct().getGifticonProductId(),
                payment.getProduct().getProductName(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getStatus().getDescription(),
                payment.getMessageCreationStatus(),
                payment.getMessageCreationStatus().getDescription(),
                payment.getMessageCreationAttemptCount(),
                payment.getLastMessageCreationAttemptAt(),
                payment.getMessageCreationFailureReason(),
                payment.getMessageRequest() == null ? null : payment.getMessageRequest().getId(),
                payment.getMessageRequest() == null ? null : payment.getMessageRequest().getStatus(),
                payment.getRefundAttemptCount(),
                payment.getLastRefundAttemptAt(),
                payment.getFailureReason(),
                payment.getApprovedAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                List.copyOf(attentionReasons),
                tossStatus,
                tossStatusMismatch,
                tossVerificationError
        );
    }

    private List<AdminGifticonPaymentResponse.AttentionReason> attentionReasons(
            GifticonPayment payment,
            AttentionThresholds thresholds
    ) {
        List<AdminGifticonPaymentResponse.AttentionReason> reasons = new ArrayList<>();
        if (payment.getStatus() == GifticonPaymentStatus.REFUND_FAILED) {
            reasons.add(attentionReason(GifticonPaymentAttentionReason.REFUND_FAILED));
        }
        if (payment.getStatus() == GifticonPaymentStatus.REFUND_PROCESSING
                && (payment.getLastRefundAttemptAt() == null
                    || payment.getLastRefundAttemptAt().isBefore(thresholds.refundStaleBefore()))) {
            reasons.add(attentionReason(
                    GifticonPaymentAttentionReason.REFUND_PROCESSING_STALE
            ));
        }
        if (payment.getRefundAttemptCount() >= thresholds.maxRefundAttempts()
                && payment.getStatus() != GifticonPaymentStatus.REFUNDED) {
            reasons.add(attentionReason(
                    GifticonPaymentAttentionReason.REFUND_ATTEMPTS_EXHAUSTED
            ));
        }
        boolean paidWithoutMessage = payment.getStatus() == GifticonPaymentStatus.PAID
                && payment.getMessageRequest() == null
                && payment.getApprovedAt() != null
                && payment.getApprovedAt().isBefore(thresholds.messageStaleBefore());
        if (paidWithoutMessage) {
            reasons.add(attentionReason(GifticonPaymentAttentionReason.PAID_WITHOUT_MESSAGE));
            reasons.add(attentionReason(GifticonPaymentAttentionReason.MESSAGE_CREATION_STALE));
        }
        if (payment.getMessageCreationStatus() == GifticonMessageCreationStatus.FAILED
                && payment.getStatus() == GifticonPaymentStatus.PAID) {
            reasons.add(attentionReason(
                    GifticonPaymentAttentionReason.MESSAGE_CREATION_FAILED_WITHOUT_REFUND
            ));
        }
        return reasons;
    }

    private AdminGifticonPaymentResponse.AttentionReason attentionReason(
            GifticonPaymentAttentionReason reason
    ) {
        return new AdminGifticonPaymentResponse.AttentionReason(
                reason.name(),
                reason.getDescription()
        );
    }

    private boolean isTossStatusMismatch(
            GifticonPaymentStatus localStatus,
            String tossStatus
    ) {
        if (!StringUtils.hasText(tossStatus)) {
            return true;
        }
        return switch (localStatus) {
            case READY, CONFIRMING -> !UNAPPROVED_TOSS_STATUSES.contains(tossStatus);
            case PAID -> !"DONE".equals(tossStatus);
            case REFUND_PENDING, REFUND_PROCESSING, REFUND_FAILED ->
                    !"DONE".equals(tossStatus);
            case REFUNDED -> !"CANCELED".equals(tossStatus);
        };
    }

    private AttentionThresholds thresholds() {
        Instant now = Instant.now();
        return new AttentionThresholds(
                now.minusSeconds(Math.max(
                        60L,
                        properties.getRequestTimeoutSeconds() * 3L
                )),
                now.minus(
                        Math.max(
                                1L,
                                properties.getUnusedPaymentRefund().getGracePeriodMinutes()
                        ),
                        ChronoUnit.MINUTES
                ),
                Math.max(1, properties.getRefundRetry().getMaxAttempts())
        );
    }

    private GifticonPayment findPayment(Long paymentId) {
        return paymentRepository.findByIdWithMessageRequest(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }
    }

    private void audit(
            AdminPrincipal admin,
            AdminAuditActionType action,
            Long paymentId,
            Object before,
            Object after,
            String reason,
            String ipAddress
    ) {
        adminAuditService.log(
                admin.adminId(),
                action,
                AdminAuditTargetType.GIFTICON_PAYMENT,
                paymentId,
                toJson(before),
                toJson(after),
                reason,
                ipAddress
        );
    }

    private void auditPaymentAction(
            AdminPrincipal admin,
            AdminAuditActionType action,
            Long paymentId,
            AdminGifticonPaymentResponse before,
            String reason,
            String ipAddress,
            RuntimeException failure
    ) {
        AdminGifticonPaymentResponse after = localResponse(findPayment(paymentId));
        Object afterValue = failure == null
                ? after
                : new FailedAdminAction(after, safeError(failure));
        audit(admin, action, paymentId, before, afterValue, reason, ipAddress);
    }

    private void validateReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("관리자 처리 사유는 필수입니다.");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("관리자 처리 사유는 500자를 초과할 수 없습니다.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String safeError(RuntimeException e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName()
                + (StringUtils.hasText(message) ? ": " + message : "");
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4)
                + "****"
                + value.substring(value.length() - 4);
    }

    private void requireAnyRole(AdminPrincipal admin, AdminRole... roles) {
        if (admin == null || !admin.hasAnyRole(roles)) {
            throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
        }
    }

    private record AttentionThresholds(
            Instant refundStaleBefore,
            Instant messageStaleBefore,
            int maxRefundAttempts
    ) {
    }

    private record FailedAdminAction(
            AdminGifticonPaymentResponse payment,
            String error
    ) {
    }
}
