package mannabom_server.manabom.presentation.gifticon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.dto.request.ConfirmGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.request.PrepareChatGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.request.PrepareGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentPrepareResponse;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentResponse;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gifticons/payments")
public class GifticonPaymentController {

    private final GifticonPaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<GifticonPaymentPrepareResponse> prepare(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PrepareGifticonPaymentRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.prepare(userId, request)
        );
    }

    @PostMapping("/chat/prepare")
    public ResponseEntity<GifticonPaymentPrepareResponse> prepareChat(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PrepareChatGifticonPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.prepareChat(userId, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<GifticonPaymentResponse> confirm(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ConfirmGifticonPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.confirm(userId, request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<GifticonPaymentResponse> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(paymentService.getPayment(userId, paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<GifticonPaymentResponse> cancelUnusedPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.cancelUnusedPayment(userId, paymentId)
        );
    }
}
