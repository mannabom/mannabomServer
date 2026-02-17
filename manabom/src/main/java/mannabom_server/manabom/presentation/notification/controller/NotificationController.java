package mannabom_server.manabom.presentation.notification.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.application.notification.service.SseService;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {
    private final SseService sseService;
    private final NotificationService notificationService;

    @GetMapping(value = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectStream(@RequestHeader(required = false, defaultValue = "", value = "Last-Event-ID") String lastEventId, @AuthenticationPrincipal Long userId){
        return sseService.createEmitterConnection(userId, lastEventId);
    }

    /**
     * 배포 전 삭제
     * **/
    @PostMapping("/test/send")
    public String sendTestNotification(@RequestBody TestNotificationRequest request) {

        // 아까 만든 통합 알림 메서드 호출 (DB저장 + SSE전송 + 실패시 Push)
        notificationService.sendNotification(
                request.targetUserId(),
                SseEventName.MATCH_FOUND, // 테스트용 이벤트 타입
                request.title(),
                request.body(),
                null // 데이터는 일단 null (필요하면 객체 넣어도 됨)
        );

        return "전송 완료! (SSE 연결된 탭을 확인하세요)";
    }

    // 테스트용 DTO (내부 클래스나 별도 파일로 생성)
    public record TestNotificationRequest(
            Long targetUserId,
            String title,
            String body
    ) {}
}
