package mannabom_server.manabom.presentation.messageRequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.messageRequest.dto.request.SendMessageRequestDto;
import mannabom_server.manabom.application.messageRequest.dto.response.SendMessageResponseDto;
import mannabom_server.manabom.application.messageRequest.service.MessageRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messageRequest")
public class MessageRequestController {
    private final MessageRequestService messageRequestService;

    @PostMapping("/send")
    public ResponseEntity<SendMessageResponseDto> sendMessage(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SendMessageRequestDto request
            ){
        return ResponseEntity.ok(
                messageRequestService.sendMessageRequest(userId, request.getTargetProfileId(), request.getMessage())
        );
    }

}
