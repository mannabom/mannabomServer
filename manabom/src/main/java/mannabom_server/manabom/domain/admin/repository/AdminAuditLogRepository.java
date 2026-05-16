package mannabom_server.manabom.domain.admin.repository;

import mannabom_server.manabom.domain.admin.entity.AdminAuditLog;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(AdminAuditTargetType targetType, Long targetId, Pageable pageable);

    @Query("""
            select log
            from AdminAuditLog log
            where log.targetId = :targetId
              and log.targetType in :targetTypes
            order by log.createdAt desc
            """)
    Page<AdminAuditLog> findUserOperationLogs(@Param("targetId") Long targetId,
                                              @Param("targetTypes") Collection<AdminAuditTargetType> targetTypes,
                                              Pageable pageable);
}
