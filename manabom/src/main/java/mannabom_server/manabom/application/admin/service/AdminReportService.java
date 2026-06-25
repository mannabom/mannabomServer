package mannabom_server.manabom.application.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminProcessReportRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminReportDetailResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminReportListResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminReportSummaryResponse;
import mannabom_server.manabom.domain.admin.entity.UserAccountRestriction;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.admin.repository.UserAccountRestrictionRepository;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.report.entity.Report;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;
import mannabom_server.manabom.domain.report.repository.ReportRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final EntityManager entityManager;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TingWalletRepository tingWalletRepository;
    private final UserAccountRestrictionRepository userAccountRestrictionRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public AdminReportListResponse searchReports(AdminPrincipal admin,
                                                 ReportStatus status,
                                                 ReportType type,
                                                 String keyword,
                                                 int page,
                                                 int size) {
        requireReportReadable(admin);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String where = buildWhere(status, type, keyword);

        String selectJpql = """
                select new mannabom_server.manabom.application.admin.dto.response.AdminReportSummaryResponse(
                    r.id,
                    r.type,
                    r.contextId,
                    reporter.userId,
                    reporter.userName,
                    reporterProfile.nickName,
                    target.userId,
                    target.userName,
                    targetProfile.nickName,
                    r.reason,
                    r.status,
                    r.createdAt,
                    r.processedAt
                )
                from Report r
                join r.reporter reporter
                join r.target target
                left join Profile reporterProfile on reporterProfile.user = reporter
                left join Profile targetProfile on targetProfile.user = target
                """ + where + " order by r.createdAt desc";

        TypedQuery<AdminReportSummaryResponse> query =
                entityManager.createQuery(selectJpql, AdminReportSummaryResponse.class);
        applySearchParams(query, status, type, keyword);
        List<AdminReportSummaryResponse> reports = query
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();

        String countJpql = """
                select count(r)
                from Report r
                join r.reporter reporter
                join r.target target
                left join Profile reporterProfile on reporterProfile.user = reporter
                left join Profile targetProfile on targetProfile.user = target
                """ + where;
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        applySearchParams(countQuery, status, type, keyword);
        long totalCount = countQuery.getSingleResult();

        return AdminReportListResponse.builder()
                .reports(reports)
                .totalCount(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / safeSize))
                .page(safePage)
                .size(safeSize)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminReportDetailResponse getReport(AdminPrincipal admin, Long reportId) {
        requireReportReadable(admin);
        return toDetail(getReportWithUsers(reportId));
    }

    @Transactional
    public AdminReportDetailResponse processReport(AdminPrincipal admin,
                                                   Long reportId,
                                                   AdminProcessReportRequest request,
                                                   String ipAddress) {
        requireReportProcessable(admin);
        Report report = getReportWithUsers(reportId);
        String before = report.getStatus().name();

        processTargetAccount(admin, report, request, ipAddress);
        grantTargetTing(admin, report, request, ipAddress);
        report.processReport(request.getAdminComment(), request.getStatus());

        adminAuditService.log(admin.adminId(), AdminAuditActionType.REPORT_PROCESS,
                AdminAuditTargetType.REPORT, report.getId(), before, report.getStatus().name(),
                request.getAdminComment(), ipAddress);
        return toDetail(report);
    }

    private void processTargetAccount(AdminPrincipal admin,
                                      Report report,
                                      AdminProcessReportRequest request,
                                      String ipAddress) {
        UserAccountStatus targetStatus = request.getTargetAccountStatus();
        if (targetStatus == null) {
            return;
        }
        validateSuspension(targetStatus, request.getTargetSuspendedUntil());
        Long targetUserId = report.getTarget().getUserId();
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("신고 대상 사용자를 찾을 수 없습니다."));
        UserAccountRestriction restriction = userAccountRestrictionRepository.findById(targetUserId)
                .orElseGet(() -> UserAccountRestriction.builder()
                        .userId(targetUserId)
                        .status(UserAccountStatus.ACTIVE)
                        .updatedByAdminId(admin.adminId())
                        .build());
        String before = accountStatusLabel(restriction);
        restriction.update(targetStatus, request.getTargetAccountReason(), request.getTargetSuspendedUntil(), admin.adminId());
        userAccountRestrictionRepository.save(restriction);
        adminAuditService.log(admin.adminId(), AdminAuditActionType.USER_STATUS_UPDATE,
                AdminAuditTargetType.USER, targetUserId, before, accountStatusLabel(restriction),
                request.getTargetAccountReason(), ipAddress);
    }

    private void grantTargetTing(AdminPrincipal admin,
                                 Report report,
                                 AdminProcessReportRequest request,
                                 String ipAddress) {
        int tingGrant = request.getTingGrant() == null ? 0 : request.getTingGrant();
        int eventTingGrant = request.getEventTingGrant() == null ? 0 : request.getEventTingGrant();
        if (tingGrant == 0 && eventTingGrant == 0) {
            return;
        }
        if (!StringUtils.hasText(request.getWalletReason())) {
            throw new IllegalArgumentException("팅 지급 사유는 필수입니다.");
        }

        Long targetUserId = report.getTarget().getUserId();
        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(targetUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(targetUserId)));
        String before = walletLabel(wallet);
        if (tingGrant > 0) {
            wallet.addTing(tingGrant);
        }
        if (eventTingGrant > 0) {
            wallet.addEventTing(eventTingGrant);
        }
        adminAuditService.log(admin.adminId(), AdminAuditActionType.WALLET_ADJUST,
                AdminAuditTargetType.TING_WALLET, targetUserId, before, walletLabel(wallet),
                request.getWalletReason(), ipAddress);
    }

    private AdminReportDetailResponse toDetail(Report report) {
        User reporter = report.getReporter();
        User target = report.getTarget();
        return AdminReportDetailResponse.builder()
                .reportId(report.getId())
                .type(report.getType())
                .contextId(report.getContextId())
                .reason(report.getReason())
                .additionalDetail(report.getAdditionalDetail())
                .status(report.getStatus())
                .adminComment(report.getAdminComment())
                .processedAt(report.getProcessedAt())
                .createdAt(report.getCreatedAt())
                .reporter(userSnapshot(reporter))
                .target(userSnapshot(target))
                .referenceContext(referenceContext(report))
                .build();
    }

    private AdminReportDetailResponse.UserSnapshot userSnapshot(User user) {
        Profile profile = profileRepository.findByUserWithRegionAndUniversity(user).orElse(null);
        TingWallet wallet = tingWalletRepository.findByUserId(user.getUserId()).orElse(null);
        UserAccountRestriction restriction = userAccountRestrictionRepository.findById(user.getUserId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        return AdminReportDetailResponse.UserSnapshot.builder()
                .userId(user.getUserId())
                .profileId(profile == null ? null : profile.getProfileId())
                .kakaoId(user.getKakaoId())
                .userName(user.getUserName())
                .phoneNum(user.getPhoneNum())
                .nickName(profile == null ? null : profile.getNickName())
                .gender(profile == null ? null : profile.getGender())
                .birthDate(profile == null ? null : profile.getBirthDate())
                .universityName(profile == null || profile.getUniversity() == null ? null : profile.getUniversity().getName())
                .regionSidoName(profile == null || profile.getRegion() == null ? null : profile.getRegion().getSidoName())
                .regionSigunguName(profile == null || profile.getRegion() == null ? null : profile.getRegion().getSigunguName())
                .verified(user.getIsVerified())
                .accountStatus(restriction == null ? UserAccountStatus.ACTIVE : restriction.effectiveStatus(now))
                .statusReason(restriction == null ? null : restriction.getReason())
                .statusSuspendedUntil(restriction == null ? null : restriction.getSuspendedUntil())
                .ting(wallet == null ? null : wallet.getTing())
                .eventTing(wallet == null ? null : wallet.getEventTing())
                .membershipActiveUntil(wallet == null ? null : wallet.getMembershipActiveUntil())
                .build();
    }

    private Map<String, Object> referenceContext(Report report) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("reportedContext", reportedContext(report));
        context.put("targetRecentReports", recentReportsByTarget(report.getTarget().getUserId(), report.getId()));
        context.put("reporterRecentReports", recentReportsByReporter(report.getReporter().getUserId(), report.getId()));
        context.put("paymentHistory", "결제 내역 테이블이 아직 없어 지갑 스냅샷과 관련 ID를 우선 제공합니다.");
        return context;
    }

    private Map<String, Object> reportedContext(Report report) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("type", report.getType());
        context.put("contextId", report.getContextId());
        if (report.getType() == ReportType.CHAT && report.getContextId() != null) {
            context.put("chatMessages", chatMessages(report.getContextId()));
        }
        if (report.getType() == ReportType.PROFILE) {
            context.put("profileOwnerUserId", report.getTarget().getUserId());
            context.put("profileOwnerNickName", userSnapshot(report.getTarget()).getNickName());
        }
        return context;
    }

    private List<?> chatMessages(Long roomId) {
        return entityManager.createQuery("""
                        select new map(
                            message.id as messageId,
                            sender.userId as senderUserId,
                            profile.nickName as senderNickName,
                            message.type as type,
                            message.content as content,
                            message.createdAt as createdAt
                        )
                        from ChatMessage message
                        join message.user sender
                        left join Profile profile on profile.user = sender
                        where message.room.id = :roomId
                        order by message.createdAt desc
                        """)
                .setParameter("roomId", roomId)
                .setMaxResults(50)
                .getResultList();
    }

    private List<?> recentReportsByTarget(Long targetUserId, Long exceptReportId) {
        return entityManager.createQuery("""
                        select new map(
                            r.id as reportId,
                            r.type as type,
                            r.reason as reason,
                            r.status as status,
                            reporter.userId as reporterUserId,
                            reporterProfile.nickName as reporterNickName,
                            r.createdAt as createdAt
                        )
                        from Report r
                        join r.reporter reporter
                        left join Profile reporterProfile on reporterProfile.user = reporter
                        where r.target.userId = :targetUserId
                          and r.id <> :exceptReportId
                        order by r.createdAt desc
                        """)
                .setParameter("targetUserId", targetUserId)
                .setParameter("exceptReportId", exceptReportId)
                .setMaxResults(10)
                .getResultList();
    }

    private List<?> recentReportsByReporter(Long reporterUserId, Long exceptReportId) {
        return entityManager.createQuery("""
                        select new map(
                            r.id as reportId,
                            r.type as type,
                            r.reason as reason,
                            r.status as status,
                            target.userId as targetUserId,
                            targetProfile.nickName as targetNickName,
                            r.createdAt as createdAt
                        )
                        from Report r
                        join r.target target
                        left join Profile targetProfile on targetProfile.user = target
                        where r.reporter.userId = :reporterUserId
                          and r.id <> :exceptReportId
                        order by r.createdAt desc
                        """)
                .setParameter("reporterUserId", reporterUserId)
                .setParameter("exceptReportId", exceptReportId)
                .setMaxResults(10)
                .getResultList();
    }

    private Report getReportWithUsers(Long reportId) {
        return reportRepository.findByIdWithUsers(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다."));
    }

    private String buildWhere(ReportStatus status, ReportType type, String keyword) {
        List<String> conditions = new ArrayList<>();
        if (status != null) {
            conditions.add("r.status = :status");
        }
        if (type != null) {
            conditions.add("r.type = :type");
        }
        if (StringUtils.hasText(keyword)) {
            conditions.add("""
                    (cast(r.id as string) = :keyword
                       or cast(reporter.userId as string) = :keyword
                       or cast(target.userId as string) = :keyword
                       or lower(reporter.userName) like :likeKeyword
                       or lower(target.userName) like :likeKeyword
                       or lower(reporterProfile.nickName) like :likeKeyword
                       or lower(targetProfile.nickName) like :likeKeyword
                       or lower(r.additionalDetail) like :likeKeyword)
                    """);
        }
        if (conditions.isEmpty()) {
            return "";
        }
        return " where " + String.join(" and ", conditions);
    }

    private void applySearchParams(TypedQuery<?> query, ReportStatus status, ReportType type, String keyword) {
        if (status != null) {
            query.setParameter("status", status);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        if (StringUtils.hasText(keyword)) {
            query.setParameter("keyword", keyword);
            query.setParameter("likeKeyword", "%" + keyword.toLowerCase() + "%");
        }
    }

    private void validateSuspension(UserAccountStatus status, LocalDateTime suspendedUntil) {
        if (status != UserAccountStatus.SUSPENDED || suspendedUntil == null) {
            return;
        }
        if (!suspendedUntil.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("정지 만료 시각은 현재 시각 이후여야 합니다.");
        }
    }

    private String accountStatusLabel(UserAccountRestriction restriction) {
        if (restriction.getSuspendedUntil() == null) {
            return restriction.getStatus().name();
        }
        return restriction.getStatus().name() + " until " + restriction.getSuspendedUntil();
    }

    private String walletLabel(TingWallet wallet) {
        return "ting=" + wallet.getTing() + ", eventTing=" + wallet.getEventTing();
    }

    private void requireReportReadable(AdminPrincipal admin) {
        if (admin.hasAnyRole(AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT, AdminRole.MODERATOR)) {
            return;
        }
        throw new IllegalStateException("신고 내역 조회 권한이 없습니다.");
    }

    private void requireReportProcessable(AdminPrincipal admin) {
        if (admin.hasAnyRole(AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT, AdminRole.MODERATOR)) {
            return;
        }
        throw new IllegalStateException("신고 처리 권한이 없습니다.");
    }
}
