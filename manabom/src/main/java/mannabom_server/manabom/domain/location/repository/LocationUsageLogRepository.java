package mannabom_server.manabom.domain.location.repository;

import mannabom_server.manabom.domain.location.entity.LocationUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LocationUsageLogRepository extends JpaRepository<LocationUsageLog, Long> {

    List<LocationUsageLog> findAllByUserIdOrderByUsedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LocationUsageLog log where log.usedAt < :usedBefore")
    int deleteAllUsedBefore(@Param("usedBefore") Instant usedBefore);
}
