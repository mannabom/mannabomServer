package mannabom_server.manabom.presentation.userInfo.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.userInfo.dto.common.UserAllPhotosDto;
import mannabom_server.manabom.application.userInfo.dto.request.DeleteUserPhotoRequest;
import mannabom_server.manabom.application.userInfo.dto.response.CheckEntitlementsResponseDto;
import mannabom_server.manabom.application.userInfo.dto.response.GetUserInfoResponse;
import mannabom_server.manabom.application.userInfo.dto.response.GetUserMainPhotoResponseDto;
import mannabom_server.manabom.application.userInfo.dto.request.PutUserInfoRequest;
import mannabom_server.manabom.application.userInfo.service.UserInfoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 내 프로필 사진 전체 조회
     * GET /api/user/all_photos
     */
    @GetMapping("/api/user/all_photos")
    public ResponseEntity<UserAllPhotosDto> getMyPhotos(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(userInfoService.getUserAllPhotos(userId));
    }

    /**
     * 내 프로필 사진 추가(업로드)
     * POST /api/user/photo
     * multipart/form-data: photo=<file>
     */
    @PostMapping(path = "/api/user/photo",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserAllPhotosDto> addMyPhoto(
            @AuthenticationPrincipal Long userId,
            @RequestPart("photo") MultipartFile photo
            ) {
        return ResponseEntity.ok(userInfoService.putUserPhoto(userId, photo));
    }

    /**
     * 내 프로필 사진 삭제
     * DELETE /api/user/photo
     */
    @DeleteMapping("/api/user/photo")
    public ResponseEntity<UserAllPhotosDto> deleteMyPhoto(
            @AuthenticationPrincipal Long userId, // 프로젝트 인증 방식에 맞게 수정
            @RequestBody @Valid DeleteUserPhotoRequest request
            ) {
        return ResponseEntity.ok(userInfoService.deleteUserPhoto(userId, request.getPhotoId()));
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     */
    @GetMapping("/api/user/active_membership")
    public ResponseEntity<Void> activeMembership(
            @AuthenticationPrincipal Long userId,
            @RequestParam("targetProfileId") Long targetProfileId
    ){
        userInfoService.activeMembership(targetProfileId);
        return ResponseEntity.status(200).build();
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     */
    @GetMapping("/api/user/add_ting")
    public ResponseEntity<Void> addTing(
            @AuthenticationPrincipal Long userId,
            @RequestParam("amount") int amount,
            @RequestParam("targetProfileId") Long targetProfileId
    ){
        userInfoService.addTing(amount, targetProfileId);

        return ResponseEntity.status(200).build();
    }
}
