package mannabom_server.manabom.presentation.meeting.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.meeting.service.MeetingVerificationService;
import mannabom_server.manabom.application.meeting.service.MeetingVerificationService.MeetingVerificationStatusData;
import mannabom_server.manabom.presentation.meeting.dto.MeetingVerificationRequest;
import mannabom_server.manabom.presentation.meeting.dto.MeetingVerificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meeting/verification")
public class MeetingVerificationController {

    private final MeetingVerificationService meetingVerificationService;

    @PostMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<MeetingVerificationResponse>> verifyMeeting(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long userId,
            @RequestBody MeetingVerificationRequest request
    ) {
        String message = meetingVerificationService.verifyMeeting(
                chatRoomId,
                userId,
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.ok(ApiResponse.success(
                new MeetingVerificationResponse(message),
                "만남 인증 요청이 처리되었습니다."
        ));
    }

    @GetMapping("/{chatRoomId}/status")
    public ResponseEntity<ApiResponse<MeetingVerificationStatusData>> getStatus(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long userId
    ) {
        MeetingVerificationStatusData data = meetingVerificationService.getStatus(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success(data, "만남 인증 상태 조회가 완료되었습니다."));
    }

}
