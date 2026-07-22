package mannabom_server.manabom.application.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.domain.notification.entity.Notification;
import mannabom_server.manabom.domain.notification.enums.NotificationType;
import mannabom_server.manabom.domain.notification.repository.NotificationRepository;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final PushService pushService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void sendNotification(
            Long userId,
            NotificationType type,
            String title,
            String message,
            Object data
    ) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .data(toJson(data))
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        pushService.sendToUser(
                userId,
                new PushMessage(title, message, convertObjectToMap(type, data))
        );
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("알림 데이터 JSON 변환 실패", e);
            return "{}";
        }
    }

    private Map<String, String> convertObjectToMap(NotificationType type, Object data) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", type.name());
        if (data == null) {
            return payload;
        }

        try {
            Map<?, ?> values = objectMapper.convertValue(data, Map.class);
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                Object value = entry.getValue();
                payload.put(
                        String.valueOf(entry.getKey()),
                        value instanceof String ? (String) value : objectMapper.writeValueAsString(value)
                );
            }
        } catch (Exception e) {
            log.warn("Push 알림 데이터 변환 실패", e);
            payload.put("data", toJson(data));
        }
        return payload;
    }
}
