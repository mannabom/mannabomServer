package mannabom_server.manabom.presentation.matching.profileMatching.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.matching.dto.request.MatchConditionRequestDto;
import mannabom_server.manabom.application.matching.profileMatching.dto.response.ProfileMatchConditionResponseDto;
import mannabom_server.manabom.application.matching.profileMatching.service.ProfileMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
