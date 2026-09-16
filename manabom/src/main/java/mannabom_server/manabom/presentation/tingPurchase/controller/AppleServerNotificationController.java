package mannabom_server.manabom.presentation.tingPurchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.port.StorePurchaseInvalidException;
import mannabom_server.manabom.application.tingPurchase.service.AppleServerNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * App Store Server Notifications V2 수신 엔드포인트.
 * App Store Connect > 앱 > 앱 정보 > App Store 서버 알림 에 이 URL 을 등록한다.
 * 인증은 Apple 이 서명한 JWS(signedPayload)를 Apple Root CA 로 검증하는 것으로 대신한다.
 * Apple 은 2xx 가 아니면 재전송하므로, 서명이 유효한 알림은 처리 결과와 무관하게 200 을 돌려준다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iap/apple")
public class AppleServerNotificationController {

    private final AppleServerNotificationService notificationService;

    @PostMapping("/notifications")
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> body) {
        Object signedPayload = body == null ? null : body.get("signedPayload");
        if (!(signedPayload instanceof String payload) || payload.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            notificationService.handle(payload);
            return ResponseEntity.ok().build();
        } catch (StorePurchaseInvalidException e) {
            log.warn("Apple 서버 알림 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            // 일시 오류는 Apple 이 재전송하도록 5xx 로 응답한다.
            log.error("Apple 서버 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
