package mannabom_server.manabom.presentation.block.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.block.dto.BlockUserRequest;
import mannabom_server.manabom.application.block.dto.BlockedUserResponse;
import mannabom_server.manabom.application.block.service.UserBlockService;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.domain.block.enums.UserBlockSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> block(
            @Valid @RequestBody BlockUserRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        userBlockService.blockByProfile(userId, request.targetProfileId(), UserBlockSource.DIRECT);
        return ResponseEntity.ok(ApiResponse.success(null, "차단했습니다."));
    }

    @DeleteMapping("/{targetProfileId}")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable Long targetProfileId,
            @AuthenticationPrincipal Long userId
    ) {
        userBlockService.unblock(userId, targetProfileId);
        return ResponseEntity.ok(ApiResponse.success(null, "차단을 해제했습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(userBlockService.getBlockedUsers(userId), "차단 목록 조회"));
    }
}
