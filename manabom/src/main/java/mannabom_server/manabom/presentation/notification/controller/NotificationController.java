package mannabom_server.manabom.presentation.notification.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.notification.enums.NotificationType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * 배포 전 삭제
     * **/
    @PostMapping("/test/send")
    public String sendTestNotification(@RequestBody TestNotificationRequest request) {

        notificationService.sendNotification(
                request.targetUserId(),
                NotificationType.MATCH_FOUND,
                request.title(),
                request.body(),
                null // 데이터는 일단 null (필요하면 객체 넣어도 됨)
        );

        return "Push 전송 완료";
    }

    // 테스트용 DTO (내부 클래스나 별도 파일로 생성)
    public record TestNotificationRequest(
            Long targetUserId,
            String title,
            String body
    ) {}
}
