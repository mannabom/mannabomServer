package mannabom_server.manabom.presentation.matching.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.matching.dto.request.MatchConditionRequestDto;
import mannabom_server.manabom.application.matching.dto.response.LoveViewMatchConditionResponseDto;
import mannabom_server.manabom.application.matching.service.LoveViewMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match/loveview")
@RequiredArgsConstructor
public class LoveViewMatchingController {
    private final LoveViewMatchService loveViewMatchService;

    @PostMapping("/simple")
    public ResponseEntity<LoveViewMatchConditionResponseDto> matchFree(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MatchConditionRequestDto request
            ){
        LoveViewMatchConditionResponseDto responseDto = loveViewMatchService.matchFree(userId, request);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/simple/extra")
    public ResponseEntity<LoveViewMatchConditionResponseDto> matchExtra(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MatchConditionRequestDto request
    ){
        LoveViewMatchConditionResponseDto responseDto = loveViewMatchService.matchExtra(userId, request);

        return ResponseEntity.ok(responseDto);
    }
}
