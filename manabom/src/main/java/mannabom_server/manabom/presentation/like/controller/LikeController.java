package mannabom_server.manabom.presentation.like.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.like.dto.request.RespondLikeRequestDto;
import mannabom_server.manabom.application.like.dto.request.SendLikeRequestDto;
import mannabom_server.manabom.application.like.dto.response.SendLikeResponseDto;
import mannabom_server.manabom.application.like.service.LikeService;
import mannabom_server.manabom.application.signal.dto.response.RespondSignalResponseDto;
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
        SendLikeResponseDto response =  likeService.sendLike(userId, request.getTargetProfileId(), request.getSource());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/respond")
    public ResponseEntity<RespondSignalResponseDto> respondLike(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RespondLikeRequestDto request
    ){
        RespondSignalResponseDto response = likeService.respondLike(
                userId,
                request.getLikeRequestId(),
                request.getAccepted(),
                request.getRejectReason()
        );

        return ResponseEntity.ok(response);
    }
}
