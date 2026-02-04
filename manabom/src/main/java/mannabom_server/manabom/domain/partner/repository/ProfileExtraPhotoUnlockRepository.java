package mannabom_server.manabom.domain.partner.repository;

import mannabom_server.manabom.domain.partner.entity.ProfileExtraPhotoUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfileExtraPhotoUnlockRepository  extends JpaRepository<ProfileExtraPhotoUnlock, Long> {
    boolean existsByRequesterUserIdAndTargetUserIdAndPhotoId(Long requesterUserId, Long targetUserId, Long photoId);

    @Query("""
        select p.photoId
        from ProfileExtraPhotoUnlock p
        where p.requesterUserId = :requesterUserId
          and p.targetUserId = :targetUserId
    """)
    List<Long> findUnlockedExtraPhotoIds(@Param("requesterUserId") Long requesterUserId,
                                    @Param("targetUserId") Long targetUserId);
}
