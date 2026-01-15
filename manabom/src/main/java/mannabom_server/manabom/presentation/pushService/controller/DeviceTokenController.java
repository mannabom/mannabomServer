package mannabom_server.manabom.presentation.pushService.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.pushService.dto.request.DeviceTokenUpsertRequest;
import mannabom_server.manabom.application.pushService.service.DeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-tokens")

public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ResponseEntity<Void> register(@AuthenticationPrincipal Long userId, @Valid @RequestBody DeviceTokenUpsertRequest req) {
        deviceTokenService.upsert(userId, req.getDeviceToken());

        log.info("디바이스 토큰 등록 처리 완료");

        return ResponseEntity.noContent().build();
    }
}

