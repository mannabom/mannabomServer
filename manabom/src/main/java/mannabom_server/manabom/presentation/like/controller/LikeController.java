package mannabom_server.manabom.presentation.like.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.like.dto.request.SendLikeRequestDto;
import mannabom_server.manabom.application.like.dto.response.SendLikeResponseDto;
import mannabom_server.manabom.application.like.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/likeRequest")
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/send")
    public ResponseEntity<SendLikeResponseDto> sendLike(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SendLikeRequestDto request
            ){
        return ResponseEntity.ok(likeService.sendLike(userId, request.getTargetProfileId()));
    }
}
