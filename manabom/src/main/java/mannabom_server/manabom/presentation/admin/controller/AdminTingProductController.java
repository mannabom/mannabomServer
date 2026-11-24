package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminTingProductRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminTingProductResponse;
import mannabom_server.manabom.application.admin.service.AdminTingProductService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ting-products")
@RequiredArgsConstructor
public class AdminTingProductController {

    private final AdminTingProductService adminTingProductService;

    @Value("${app.admin.audit.trust-proxy:false}")
    private boolean trustProxy;

    @Value("${app.admin.audit.trusted-proxies:}")
    private String trustedProxies;

    @GetMapping
    public ResponseEntity<List<AdminTingProductResponse>> getProducts(@AuthenticationPrincipal AdminPrincipal admin) {
        return ResponseEntity.ok(adminTingProductService.getProducts(admin));
    }

    @PostMapping
    public ResponseEntity<AdminTingProductResponse> create(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminTingProductRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminTingProductService.create(admin, request, clientIp(httpRequest)));
    }

    @PutMapping("/{tingProductId}")
    public ResponseEntity<AdminTingProductResponse> update(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long tingProductId,
            @RequestBody @Valid AdminTingProductRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminTingProductService.update(admin, tingProductId, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustProxy || !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return Arrays.stream(forwarded.split(","))
                    .map(String::trim)
                    .filter(ip -> !ip.isBlank())
                    .filter(ip -> !"unknown".equalsIgnoreCase(ip))
                    .findFirst()
                    .orElse(remoteAddr);
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank() || trustedProxies == null || trustedProxies.isBlank()) {
            return false;
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .anyMatch(remoteAddr::equals);
    }
}
