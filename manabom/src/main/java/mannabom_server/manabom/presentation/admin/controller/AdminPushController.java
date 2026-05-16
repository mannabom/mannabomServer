package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminSendPushRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminPushResponse;
import mannabom_server.manabom.application.admin.service.AdminPushService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/push")
public class AdminPushController {

    private final AdminPushService adminPushService;

    @PostMapping
    public AdminPushResponse sendPush(@AuthenticationPrincipal AdminPrincipal principal,
                                      @Valid @RequestBody AdminSendPushRequest request,
                                      HttpServletRequest httpRequest) {
        return adminPushService.sendPush(principal, request, httpRequest.getRemoteAddr());
    }
}
