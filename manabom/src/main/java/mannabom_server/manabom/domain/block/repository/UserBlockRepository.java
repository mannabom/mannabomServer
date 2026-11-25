package mannabom_server.manabom.domain.block.repository;

import mannabom_server.manabom.domain.block.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    List<UserBlock> findAllByBlockerUserIdOrderByCreatedAtDesc(Long blockerUserId);

    /** 두 회원 사이에 어느 방향이든 차단이 있는지 */
    @Query("""
            select count(b) > 0
              from UserBlock b
             where (b.blockerUserId = :userId and b.blockedUserId = :otherUserId)
                or (b.blockerUserId = :otherUserId and b.blockedUserId = :userId)
            """)
    boolean existsBetween(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    /** 내가 차단했거나 나를 차단한 회원 ID 전체 */
    @Query("""
            select case when b.blockerUserId = :userId then b.blockedUserId else b.blockerUserId end
              from UserBlock b
             where b.blockerUserId = :userId or b.blockedUserId = :userId
            """)
    Set<Long> findBlockedUserIdsInEitherDirection(@Param("userId") Long userId);
}
