package mannabom_server.manabom.domain.region.repository;

import mannabom_server.manabom.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region,String> {
    @Query(
            "select r from Region r "+
                    "where r.sidoName =:sido and "+
            "r.sigunguName = :sigungu"
    )
    Optional<Region> findBySidoNameAndSigunguName(@Param(value = "sido") String sido, @Param(value = "sigungu") String sigungu);
}
