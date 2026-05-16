package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.response.AdminAuditLogListResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminAuditLogResponse;
import mannabom_server.manabom.domain.admin.entity.AdminAccount;
import mannabom_server.manabom.domain.admin.entity.AdminAuditLog;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.admin.repository.AdminAccountRepository;
import mannabom_server.manabom.domain.admin.repository.AdminAuditLogRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAccountRepository adminAccountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long adminId,
                    AdminAuditActionType actionType,
                    AdminAuditTargetType targetType,
                    Long targetId,
                    String beforeValue,
                    String afterValue,
                    String reason,
                    String ipAddress) {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminId(adminId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .reason(reason)
                .ipAddress(ipAddress)
                .build());
    }

    @Transactional(readOnly = true)
    public AdminAuditLogListResponse getLogs(AdminPrincipal admin, int page, int size) {
        requireAuditReadable(admin);
        return toListResponse(adminAuditLogRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public AdminAuditLogListResponse getUserOperationLogs(AdminPrincipal admin, Long userId, int page, int size) {
        requireAuditReadable(admin);
        return toListResponse(adminAuditLogRepository.findUserOperationLogs(userId,
                List.of(AdminAuditTargetType.USER, AdminAuditTargetType.TING_WALLET, AdminAuditTargetType.PUSH),
                pageRequest(page, size)));
    }

    private AdminAuditLogListResponse toListResponse(Page<AdminAuditLog> page) {
        Map<Long, AdminAccount> admins = adminAccountRepository.findAllById(
                        page.getContent().stream().map(AdminAuditLog::getAdminId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(AdminAccount::getAdminId, Function.identity()));
        return AdminAuditLogListResponse.builder()
                .logs(page.getContent().stream().map(log -> toResponse(log, admins.get(log.getAdminId()))).toList())
                .totalCount(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log, AdminAccount admin) {
        return AdminAuditLogResponse.builder()
                .auditId(log.getAuditId())
                .adminId(log.getAdminId())
                .adminLoginId(admin == null ? null : admin.getLoginId())
                .adminName(admin == null ? null : admin.getName())
                .actionType(log.getActionType())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .beforeValue(log.getBeforeValue())
                .afterValue(log.getAfterValue())
                .reason(log.getReason())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    private void requireAuditReadable(AdminPrincipal admin) {
        if (admin.hasAnyRole(AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT)) {
            return;
        }
        throw new SecurityException("감사 로그를 조회할 권한이 없습니다.");
    }
}
