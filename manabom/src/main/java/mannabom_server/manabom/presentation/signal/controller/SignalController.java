package mannabom_server.manabom.presentation.signal.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeResponseDto;
import mannabom_server.manabom.application.signal.dto.response.SignalToMeResponseDto;
import mannabom_server.manabom.application.signal.service.SignalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interest")
public class SignalController {
    private final SignalService signalService;

    @GetMapping("/received")
    public ResponseEntity<SignalToMeResponseDto> getSignalToMe(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(signalService.getSignalsToMe(userId));
    }

    @GetMapping("sent")
    public ResponseEntity<SignalFromMeResponseDto> getSignalFromMe(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(signalService.getSignalsFromMe(userId));
    }
}
