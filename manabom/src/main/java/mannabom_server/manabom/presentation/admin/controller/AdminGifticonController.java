package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminConfigureGifticonTokenRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonProductResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonProductSliceResponse;
import mannabom_server.manabom.application.admin.service.AdminGifticonService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/gifticons")
@RequiredArgsConstructor
public class AdminGifticonController {

    private final AdminGifticonService adminGifticonService;

    @GetMapping
    public ResponseEntity<AdminGifticonProductSliceResponse> getProducts(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminGifticonService.getProducts(admin, cursor, size));
    }

    @PutMapping("/{gifticonProductId}/template-token")
    public ResponseEntity<AdminGifticonProductResponse> configureTemplateToken(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long gifticonProductId,
            @RequestBody @Valid AdminConfigureGifticonTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminGifticonService.configureTemplateToken(
                admin,
                gifticonProductId,
                request,
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
