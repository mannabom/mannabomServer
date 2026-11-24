package mannabom_server.manabom.application.tingPurchase.port;

import java.time.Instant;

/** App Store Server Notifications V2 의 signedPayload 를 검증·복호화한다. */
public interface AppleServerNotificationDecoder {

    /**
     * @throws StorePurchaseInvalidException 서명 검증 실패 등 신뢰할 수 없는 알림
     */
    AppleNotification decode(String signedPayload);

    record AppleNotification(
            String notificationType,
            String subtype,
            String notificationUuid,
            String environment,
            String transactionId,
            String originalTransactionId,
            String productId,
            Instant revocationDate,
            String revocationReason
    ) {
    }
}
