package mannabom_server.manabom.application.tingPurchase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.port.AppleServerNotificationDecoder;
import mannabom_server.manabom.application.tingPurchase.port.AppleServerNotificationDecoder.AppleNotification;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * App Store Server Notifications V2 처리.
 * 소모성 팅 상품에 의미 있는 알림은 REFUND / REVOKE 이며, 나머지는 기록만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleServerNotificationService {

    private static final String REFUND = "REFUND";
    private static final String REVOKE = "REVOKE";
    private static final String TEST = "TEST";

    private final AppleServerNotificationDecoder decoder;
    private final TingPurchaseRefundService refundService;

    public void handle(String signedPayload) {
        if (signedPayload == null || signedPayload.isBlank()) {
            throw new IllegalArgumentException("signedPayload는 필수입니다.");
        }
        AppleNotification notification = decoder.decode(signedPayload);
        String type = notification.notificationType();
        log.info("Apple 서버 알림 수신. type={}, subtype={}, uuid={}, env={}, transactionId={}",
                type, notification.subtype(), notification.notificationUuid(),
                notification.environment(), notification.transactionId());

        if (REFUND.equals(type) || REVOKE.equals(type)) {
            String reason = type + (notification.revocationReason() == null ? "" : ":" + notification.revocationReason());
            Instant refundedAt = notification.revocationDate() == null ? Instant.now() : notification.revocationDate();
            TingPurchaseRefundService.RefundOutcome outcome = refundService.refundByTransactionId(
                    PurchaseStore.APPLE,
                    notification.transactionId(),
                    reason,
                    refundedAt
            );
            log.info("Apple 환불 반영 결과. transactionId={}, outcome={}", notification.transactionId(), outcome);
            return;
        }
        if (TEST.equals(type)) {
            log.info("Apple 테스트 알림 수신 확인.");
        }
    }
}
