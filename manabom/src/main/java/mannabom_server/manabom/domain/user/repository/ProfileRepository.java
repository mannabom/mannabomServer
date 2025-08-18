package mannabom_server.manabom.domain.user.repository;

import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    /**
     * 사용자로 프로필 조회
     */
    Optional<Profile> findByUser(User user);

    /**
     * 닉네임으로 프로필 조회
     */
    Optional<Profile> findByNickName(String nickName);

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickName(String nickName);
}
