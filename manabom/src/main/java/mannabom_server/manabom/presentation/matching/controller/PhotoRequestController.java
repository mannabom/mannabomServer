package mannabom_server.manabom.presentation.matching.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.matching.dto.response.LoveViewPhotoStatusResponse;
import mannabom_server.manabom.application.matching.service.PhotoRequestService;
import mannabom_server.manabom.domain.matching.enums.LoveViewPhotoStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loveview/profile")
public class PhotoRequestController {
    private final PhotoRequestService photoRequestService;

    @GetMapping("/status/{chatRoomId}")
    public ResponseEntity<ApiResponse<LoveViewPhotoStatusResponse>> getProfileStatus(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId){
        LoveViewPhotoStatus status = photoRequestService.getPhotoRequestStatus(chatRoomId,userId);
        String systemMessage = null;
        switch(status){
            case WAITING -> systemMessage = "서로를 알아가는 단계예요. 대화를 조금 더 나눠보세요! (10마디 미만)";
            case READY -> systemMessage ="서로 충분히 대화했네요! 이제 상대방에게 프로필 사진을 요청해 볼까요?";
            case PENDING -> systemMessage = "상대방의 수락을 기다리고 있어요. 조금만 기다려 주세요!";
            case RECEIVED -> systemMessage = "상대방이 프로필 사진 공개를 요청했어요! 수락하시겠어요?";
            case ACCEPTED -> systemMessage = "서로의 프로필이 공개되었습니다. 즐거운 대화 나누세요! ✨";
            case REJECTED -> systemMessage ="상대방이 아직은 조심스러운가 봐요. 10마디 더 나누면 다시 요청할 수 있어요.";
        }

        return ResponseEntity.ok(ApiResponse.success(new LoveViewPhotoStatusResponse(chatRoomId, status, systemMessage), "프로필 사진 요청 상태 조회에 성공했습니다."));
    }

    @PatchMapping("/accept/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> acceptPhotoRequest(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId){
        photoRequestService.acceptPhotoRequest(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "프로필 사진 요청을 수락하였습니다."));
    }
    @PatchMapping("/reject/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> rejectPhotoRequest(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId){
        photoRequestService.rejectPhotoRequest(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "프로필 사진 거절을 수락하였습니다."));
    }


}
