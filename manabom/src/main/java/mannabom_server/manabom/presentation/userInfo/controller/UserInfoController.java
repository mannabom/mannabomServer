package mannabom_server.manabom.presentation.userInfo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.userInfo.dto.GetUserInfoResponse;
import mannabom_server.manabom.application.userInfo.dto.PutUserInfoRequest;
import mannabom_server.manabom.application.userInfo.service.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

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
            PutUserInfoRequest request
    ){
        userInfoService.putUserInfo(userId, request);

        return ResponseEntity.status(200).build();
    }


}
