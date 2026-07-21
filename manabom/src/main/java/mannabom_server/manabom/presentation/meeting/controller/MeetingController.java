package mannabom_server.manabom.presentation.meeting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.meeting.dto.response.MeetingPage;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomCreateRequest;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomJoinByCodeRequest;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomsSearchRequest;
import mannabom_server.manabom.application.meeting.dto.response.MeetingRoomCreateDataDto;
import mannabom_server.manabom.application.meeting.dto.response.TeamMemberProfilesDto;
import mannabom_server.manabom.application.meeting.dto.response.MyMeetingStatusDataDto;
import jakarta.validation.Valid;
import mannabom_server.manabom.application.meeting.dto.request.MeetingCancellationVoteRequest;
import mannabom_server.manabom.application.meeting.dto.response.MeetingCancellationResponse;
import mannabom_server.manabom.application.meeting.service.MeetingCancellationService;
import mannabom_server.manabom.application.meeting.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;
    private final MeetingCancellationService meetingCancellationService;

    /*미팅방 생성 api*/
    @PostMapping("/rooms/create")
    public ResponseEntity<ApiResponse<MeetingRoomCreateDataDto>> createMeeting(@RequestBody MeetingRoomCreateRequest request, @AuthenticationPrincipal Long userId){
        MeetingRoomCreateDataDto data = meetingService.create(request,userId);
        return ResponseEntity.ok(ApiResponse.success(data, "방 생성 완료"));
    }

    /*미팅 방 코드로 입장 api*/
    @PostMapping("/rooms/join-by-code")
    public ResponseEntity<ApiResponse<MeetingRoomCreateDataDto>> joinMeetingByCode(@RequestBody MeetingRoomJoinByCodeRequest request, @AuthenticationPrincipal Long userId){
        MeetingRoomCreateDataDto data = meetingService.enterRoomByCode(request,userId);
        return ResponseEntity.ok(ApiResponse.success(data, "방 입장 완료"));
    }

    /*미팅 방 입장(빠른매칭 api)*/
    @PostMapping("/rooms/join/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingRoomCreateDataDto>> joinMeetingById(@PathVariable Long meetingId, @AuthenticationPrincipal Long userId){
        MeetingRoomCreateDataDto data = meetingService.enterRoomById(meetingId,userId);
        return ResponseEntity.ok(ApiResponse.success(data, "방 입장 완료"));
    }

    /*내 미팅 상태 조회 api*/
    @GetMapping("/my-status")
    public ResponseEntity<ApiResponse<MyMeetingStatusDataDto>> getMyMeeting(@AuthenticationPrincipal Long userId){
        MyMeetingStatusDataDto dto = meetingService.checkUserMeetingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(dto, "내 미팅 상태 조회 성공"));
    }

    @PostMapping("/matches/{matchId}/cancellation-requests")
    public ResponseEntity<ApiResponse<MeetingCancellationResponse>> createCancellationRequest(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Long userId
    ) {
        MeetingCancellationResponse response = meetingCancellationService.create(matchId, userId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "미팅 전체 취소 요청을 시작했습니다.")
        );
    }

    @PostMapping("/cancellation-requests/{requestId}/votes")
    public ResponseEntity<ApiResponse<MeetingCancellationResponse>> voteCancellation(
            @PathVariable Long requestId,
            @Valid @RequestBody MeetingCancellationVoteRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        MeetingCancellationResponse response = meetingCancellationService.vote(
                requestId,
                userId,
                request.getDecision()
        );
        return ResponseEntity.ok(
                ApiResponse.success(response, "미팅 전체 취소 투표를 완료했습니다.")
        );
    }

    @GetMapping("/matches/{matchId}/cancellation-requests/current")
    public ResponseEntity<ApiResponse<MeetingCancellationResponse>> getCurrentCancellationRequest(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Long userId
    ) {
        MeetingCancellationResponse response = meetingCancellationService.getCurrent(matchId, userId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "진행 중인 미팅 전체 취소 요청을 조회했습니다.")
        );
    }

    /*팀원 프로필 상세 조회*/
    @GetMapping("/member-profiles/{meetingId}")
    public ResponseEntity<ApiResponse<TeamMemberProfilesDto>> getMeetingMemberProfiles(@PathVariable Long meetingId, @AuthenticationPrincipal Long userId){
        TeamMemberProfilesDto dto =meetingService.getTeamMemberProfilesDetail(meetingId);
        return ResponseEntity.ok(ApiResponse.success(dto, "미팅 팀원 프로필 상세 조회 성공"));
    }


    /*조견별 방 리스트 조회*/
    @GetMapping("/rooms/search")
    public ResponseEntity<ApiResponse<MeetingPage>> getMeetingList(
            @RequestBody MeetingRoomsSearchRequest request,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String cursorToken
            ){
        MeetingPage page = meetingService.getMeetingList(request,pageSize,cursorToken);
        return ResponseEntity.ok(ApiResponse.success(page,"미팅 리스트 출력 완료"));
    }

}
