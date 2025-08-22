package mannabom_server.manabom.domain.signup.repository;

import mannabom_server.manabom.domain.signup.entity.SignupProgress;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Redis 기반 회원가입 진행 상태 관리 레포지토리 - JPA 사용 X
 */
@Repository
public interface SignupProgressRepository extends CrudRepository<SignupProgress, String> {
    // 기본 CRUD 메서드만 사용:
    // - save(entity)
    // - findById(profileId)
    // - existsById(profileId)
    // - delete(entity)
    // - deleteById(profileId)
}