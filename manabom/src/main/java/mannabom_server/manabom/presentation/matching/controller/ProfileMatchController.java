package mannabom_server.manabom.presentation.matching.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.matching.dto.request.MatchConditionRequestDto;
import mannabom_server.manabom.application.matching.dto.request.ProfileRatingRequestDto;
import mannabom_server.manabom.application.matching.dto.response.ProfileMatchConditionResponseDto;
import mannabom_server.manabom.application.matching.dto.response.RecommendedTodayProfileListResponseDto;
import mannabom_server.manabom.application.matching.service.ProfileMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match/profile")
@RequiredArgsConstructor
public class ProfileMatchController {

    private final ProfileMatchService profileMatchService;

    @PostMapping("/simple")
    public ResponseEntity<ProfileMatchConditionResponseDto> matchFree(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MatchConditionRequestDto request
            ){
        return ResponseEntity.ok(profileMatchService.matchFree(userId, request));
    }

    @PostMapping("/simple/extra")
    public ResponseEntity<ProfileMatchConditionResponseDto> matchExtra(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MatchConditionRequestDto request
    ){
        return ResponseEntity.ok(profileMatchService.matchExtra(userId, request));
    }

    @PostMapping("/rate")
    public ResponseEntity<Void> rateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ProfileRatingRequestDto request
            ) {
        profileMatchService.rate(userId, request.getTargetProfileId(), request.getScore());

        return ResponseEntity.status(204).build();
    }

    @GetMapping("/simple/today")
    public ResponseEntity<RecommendedTodayProfileListResponseDto> getRecommendedTodayProfileList(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(profileMatchService.getRecommendedTodayProfileList(userId));
    }
}
