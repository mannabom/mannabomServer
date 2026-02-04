package mannabom_server.manabom.domain.notification.repository;

import mannabom_server.manabom.domain.notification.entity.SseEventCache;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SseEventCacheRepository extends CrudRepository<SseEventCache,String> {
    List<SseEventCache> findAllByUserId(Long userId);
}
