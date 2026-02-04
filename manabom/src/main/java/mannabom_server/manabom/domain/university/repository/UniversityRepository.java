package mannabom_server.manabom.domain.university.repository;

import mannabom_server.manabom.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    /**
     * 도메인으로 대학 조회
     */
    Optional<University> findByDomain(String domain);

    /**
     * 도메인 존재 여부 확인
     */
    boolean existsByDomain(String domain);

    /**
     * 대학 이름으로 대학 조회
     */
    Optional<University> findByName(String name);

}
