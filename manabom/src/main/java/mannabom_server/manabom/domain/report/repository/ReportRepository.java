package mannabom_server.manabom.domain.report.repository;

import mannabom_server.manabom.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
