package mannabom_server.manabom.domain.report.repository;

import mannabom_server.manabom.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("""
            select r
            from Report r
            join fetch r.reporter
            join fetch r.target
            where r.id = :reportId
            """)
    Optional<Report> findByIdWithUsers(@Param("reportId") Long reportId);
}
