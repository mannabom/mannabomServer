package mannabom_server.manabom.presentation.userInfo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.userInfo.dto.CheckEntitlementsResponseDto;
import mannabom_server.manabom.application.userInfo.dto.GetUserInfoResponse;
import mannabom_server.manabom.application.userInfo.dto.GetUserMainPhotoResponseDto;
import mannabom_server.manabom.application.userInfo.dto.PutUserInfoRequest;
import mannabom_server.manabom.application.userInfo.service.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
public class UserInfoController {

    private final UserInfoService userInfoService;

    @GetMapping("/api/user/info")
    public ResponseEntity<GetUserInfoResponse> getUserInfo(
            @AuthenticationPrincipal Long userId
    ){
        log.info("회원 정보 조회 api 동작 시작");

        GetUserInfoResponse userInfoResponse = userInfoService.getUserInfo(userId);

        log.info("회원 정보 조회 api 동작 완료");

        return ResponseEntity.ok(userInfoResponse);
    }

    @PutMapping("/api/user/info")
    public ResponseEntity<Void> putUserInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody PutUserInfoRequest request
    ){
        userInfoService.putUserInfo(userId, request);

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/api/user/main_photo")
    public ResponseEntity<GetUserMainPhotoResponseDto> getUserMainPhoto(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(userInfoService.getUserMainPhoto(userId));
    }

    @GetMapping("/api/user/entitlements")
    public ResponseEntity<CheckEntitlementsResponseDto> checkEntitlements(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(userInfoService.checkEntitlements(userId));
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     */
    @GetMapping("/api/user/active_membership")
    public ResponseEntity<Void> activeMembership(
            @AuthenticationPrincipal Long userId
    ){
        userInfoService.activeMembership(userId);
        return ResponseEntity.status(200).build();
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     */
    @GetMapping("/api/user/add_ting")
    public ResponseEntity<Void> addTing(
            @AuthenticationPrincipal Long userId,
            @RequestParam("amount") int amount
    ){
        userInfoService.addTing(userId, amount);

        return ResponseEntity.status(200).build();
    }
}
