package mannabom_server.manabom.domain.report.repository;

import mannabom_server.manabom.domain.report.entity.Report;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findFirstByReporter_UserIdAndTarget_UserIdAndTypeAndContextIdAndStatusInOrderByIdAsc(
            Long reporterId,
            Long targetId,
            ReportType type,
            Long contextId,
            Collection<ReportStatus> statuses
    );

    @Query("""
            select r
            from Report r
            join fetch r.reporter
            join fetch r.target
            where r.id = :reportId
            """)
    Optional<Report> findByIdWithUsers(@Param("reportId") Long reportId);
}
