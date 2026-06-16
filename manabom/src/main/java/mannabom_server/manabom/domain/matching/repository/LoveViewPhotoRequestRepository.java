package mannabom_server.manabom.domain.matching.repository;

import mannabom_server.manabom.domain.matching.entity.LoveViewPhotoRequest;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveViewPhotoRequestRepository extends JpaRepository<LoveViewPhotoRequest,Long> {

    Optional<LoveViewPhotoRequest> findTopByHistoryIdOrderByIdDesc(Long historyId);

}
