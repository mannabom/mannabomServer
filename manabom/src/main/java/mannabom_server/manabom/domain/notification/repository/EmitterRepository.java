package mannabom_server.manabom.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EmitterRepository {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30*60*1000L; // 타임아웃 30분


    public SseEmitter save(Long userId){
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);
        return emitter;
    }

    public void remove(Long userId){
        emitters.remove(userId);
    }

    public SseEmitter getById(Long userId){
        return emitters.get(userId);
    }
}
