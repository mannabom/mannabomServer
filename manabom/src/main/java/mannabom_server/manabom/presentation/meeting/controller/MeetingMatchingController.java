package mannabom_server.manabom.presentation.meeting.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.notification.service.SseService;
import mannabom_server.manabom.application.meeting.dto.response.*;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.application.meeting.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/meeting/matching")
public class MeetingMatchingController {
    private final SseService sseService;
    private final MeetingService meetingService;
    private final MeetingMatchingService meetingMatchingService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<MatchingStartDataDto>> startMatching( @AuthenticationPrincipal Long userId){
        MatchingStartDataDto data = meetingService.startMatching(userId);
        return ResponseEntity.ok(ApiResponse.success(data,"매칭 신청이 완료되었습니다."));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<MatchingCancelDataDto>> cancelMatching(@AuthenticationPrincipal Long userId){
        boolean data = meetingService.cancelMatching(userId);
        return ResponseEntity.ok(ApiResponse.success(new MatchingCancelDataDto(data),"매칭 취소가 완료되었습니다."));
    }

    @PostMapping("/reject/{matchId}")
    public ResponseEntity<ApiResponse<RejectMatchDataDto>> rejectMatching(@PathVariable Long matchId, @AuthenticationPrincipal Long userId){
        RejectMatchDataDto data = meetingMatchingService.rejectMatch(matchId,userId);
        return ResponseEntity.ok(ApiResponse.success(data,"매칭 거절이 완료되었습니다."));
    }

    @PostMapping("/accept/{matchId}")
    public ResponseEntity<ApiResponse<AcceptMatchDataDto>> acceptMatching(@PathVariable Long matchId, @AuthenticationPrincipal Long userId){
        AcceptMatchDataDto data = meetingMatchingService.acceptMatch(matchId,userId);
        return ResponseEntity.ok(ApiResponse.success(data, "매칭 수락이 완료되었습니다."));
    }

    @GetMapping("/result/{matchId}")
    public ResponseEntity<ApiResponse<MatchingResultDataDto>> getMatchingResult(@PathVariable Long matchId, @AuthenticationPrincipal Long userId){
        MatchingResultDataDto data = meetingMatchingService.getMatchingResult(matchId, userId);
        return ResponseEntity.ok(ApiResponse.success(data, "매칭 결과 조회가 완료되었습니다."));
    }


}
