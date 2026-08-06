package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminGifticonPaymentActionRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonPaymentPageResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonPaymentResponse;
import mannabom_server.manabom.application.admin.service.AdminGifticonPaymentService;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/gifticon-payments")
@RequiredArgsConstructor
public class AdminGifticonPaymentController {

    private final AdminGifticonPaymentService paymentService;

    @GetMapping
    public ResponseEntity<AdminGifticonPaymentPageResponse> getPayments(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) GifticonPaymentStatus paymentStatus,
            @RequestParam(required = false) GifticonMessageCreationStatus messageCreationStatus,
            @RequestParam(required = false) MessageRequestStatus messageRequestStatus,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderId,
            @RequestParam(defaultValue = "false") boolean attentionOnly
    ) {
        return ResponseEntity.ok(paymentService.getPayments(
                admin,
                page,
                size,
                paymentStatus,
                messageCreationStatus,
                messageRequestStatus,
                userId,
                orderId,
                attentionOnly
        ));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<AdminGifticonPaymentResponse> getPayment(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(paymentService.getPayment(admin, paymentId));
    }

    @PostMapping("/{paymentId}/retry-message")
    public ResponseEntity<AdminGifticonPaymentResponse> retryMessage(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long paymentId,
            @Valid @RequestBody AdminGifticonPaymentActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(paymentService.retryMessage(
                admin,
                paymentId,
                request.reason().trim(),
                clientIp(httpRequest)
        ));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<AdminGifticonPaymentResponse> forceRefund(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long paymentId,
            @Valid @RequestBody AdminGifticonPaymentActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(paymentService.forceRefund(
                admin,
                paymentId,
                request.reason().trim(),
                clientIp(httpRequest)
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
