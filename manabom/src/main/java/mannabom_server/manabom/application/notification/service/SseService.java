package mannabom_server.manabom.application.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import mannabom_server.manabom.domain.notification.entity.SseEventCache;
import mannabom_server.manabom.domain.notification.repository.EmitterRepository;
import mannabom_server.manabom.domain.notification.repository.SseEventCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final EmitterRepository emitterRepository;
    private final SseEventCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    public SseEmitter createEmitterConnection(Long userId, String lastEventId) {
        SseEmitter emitter = emitterRepository.save(userId);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료 : {}", userId);
            emitterRepository.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임 아웃: {}", userId);
            emitterRepository.remove(userId);
        });

        emitter.onError((e) -> {
            log.info("SSE 연결 에러:{}", userId);
            emitterRepository.remove(userId);
        });

        sendToUserBySse(userId, userId + "_" + System.currentTimeMillis(), SseEventName.CONNECTED.getDescription(), SseEventName.CONNECTED);

        //안보내진 알림 보내기
        if (lastEventId != null && !lastEventId.isEmpty()) {
            sendLostMessage(userId, lastEventId);
        }

        return emitter;

    }
    /**
     * 알림 전송 (외부 호출용)
     * return: true(전송 성공), false(전송 실패 -> Push 알림 필요)
     */
    public boolean send(Long userId, SseEventName eventName, Object data) {
        String eventId = userId + "_" + System.currentTimeMillis();
        saveEventToRedis(userId, eventId, eventName, data);

       return sendToUserBySse(userId, eventId, eventName.getDescription(), data);
    }

    /**
     * Redis 저장 로직
     */
    private void saveEventToRedis(Long userId, String eventId, SseEventName eventName, Object data) {
        try {
            String dataJson = objectMapper.writeValueAsString(data);
            SseEventCache eventCache = SseEventCache.builder()
                    .eventName(eventName.getDescription())
                    .userId(userId)
                    .id(eventId)
                    .data(dataJson)
                    .build();

            cacheRepository.save(eventCache);
        } catch (JsonProcessingException e) {
            log.error("SSE 데이터 Json 변환 실패 ");
        }
    }

    /**
     * 유실 데이터 재전송 로직
     */
    private void sendLostMessage(Long userId, String lastEventId) {
        List<SseEventCache> list = cacheRepository.findAllByUserId(userId);
        list.stream()
                .filter(event -> event.getId().compareTo(lastEventId) > 0)
                .forEach(sseEventCache -> sendToUserBySse(userId, sseEventCache.getId(), sseEventCache.getEventName(), sseEventCache.getData()));
    }

    /**
     * 실제 Emitter 전송 로직
     */
    private boolean sendToUserBySse(Long userId, String eventId, String eventName, Object data) {
        SseEmitter emitter = emitterRepository.getById(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(eventName)
                        .data(data)
                );
                return true;
            } catch (IOException e) {
                emitterRepository.remove(userId);
                log.error("SSE 전송 에러 :{} userId :{}", e, userId);
                return false;
            }
        }
        return false;
    }

}
