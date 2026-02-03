package mannabom_server.manabom.domain.likeRequest.repository;

import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRequestRepository extends JpaRepository<LikeRequest, Long> {

    Optional<LikeRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    List<LikeRequest> findAllByToUserIdAndStatus(Long toUserId, LikeStatus status);

    List<LikeRequest> findAllByFromUserIdAndStatus(Long fromUserId, LikeStatus status);

    List<LikeRequest> findAllByToUserId(Long toUserId);

    List<LikeRequest> findAllByFromUserId(Long fromUserId);
}
