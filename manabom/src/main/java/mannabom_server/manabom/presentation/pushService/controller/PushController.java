package mannabom_server.manabom.presentation.pushService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.pushService.dto.request.SendPushRequestDto;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push")
@Slf4j
public class PushController {

    private final PushService pushService;

    @PostMapping("/user/{userId}")
    public void pushUser(@PathVariable Long userId, @RequestBody SendPushRequestDto request) {
        log.info("push controller 계층 호춮\nuserId:{}, title:{}", userId, request.getTitle());
        pushService.sendToUser(userId, new PushMessage(request.getTitle(), request.getBody(), null));
        log.info("push controller 계층 처리 완료");
    }

    @PostMapping("/broadcast")
    public void broadcast(@RequestBody SendPushRequestDto request) {
        pushService.broadcastToAll(new PushMessage(request.getTitle(), request.getBody(), null));
    }
}

