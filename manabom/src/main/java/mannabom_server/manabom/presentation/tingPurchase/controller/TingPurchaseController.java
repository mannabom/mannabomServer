package mannabom_server.manabom.presentation.tingPurchase.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.tingPurchase.dto.request.VerifyTingPurchaseRequest;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingProductCatalogResponse;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingPurchaseResponse;
import mannabom_server.manabom.application.tingPurchase.service.TingProductService;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 팅 인앱결제 API.
 * 1. GET  /api/ting/products                -> 상품 목록 + 결제 시 넣을 purchaseAccountToken
 * 2. (앱) StoreKit 2 / Play Billing 으로 결제
 * 3. POST /api/ting/purchases/verify        -> 서버가 스토어에 검증하고 팅 지급
 * 4. (앱) Apple 은 Transaction.finish(), Google 은 서버가 consume 하므로 추가 작업 없음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ting")
public class TingPurchaseController {

    private final TingProductService productService;
    private final TingPurchaseService purchaseService;

    @GetMapping("/products")
    public ResponseEntity<TingProductCatalogResponse> getProducts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(productService.getCatalog(userId));
    }

    @PostMapping("/purchases/verify")
    public ResponseEntity<TingPurchaseResponse> verify(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyTingPurchaseRequest request
    ) {
        return ResponseEntity.ok(purchaseService.verify(userId, request));
    }

    @GetMapping("/purchases/{paymentId}")
    public ResponseEntity<TingPurchaseResponse> getPurchase(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(purchaseService.getPurchase(userId, paymentId));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<TingPurchaseResponse>> getRecentPurchases(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(purchaseService.getRecentPurchases(userId, size));
    }
}
