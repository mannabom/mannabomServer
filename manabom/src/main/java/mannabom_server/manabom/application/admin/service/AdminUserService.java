package mannabom_server.manabom.application.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdateUserStatusRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminUserDetailResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminUserListResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminUserSummaryResponse;
import mannabom_server.manabom.domain.admin.entity.UserAccountRestriction;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.admin.repository.UserAccountRestrictionRepository;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;
    private final TingWalletRepository tingWalletRepository;
    private final UserAccountRestrictionRepository userAccountRestrictionRepository;
    private final AdminAuditService adminAuditService;
    private final FileStoragePort fileStoragePort;

    @Transactional(readOnly = true)
    public AdminUserListResponse searchUsers(AdminPrincipal admin,
                                             String keyword,
                                             String searchType,
                                             String accountStatus,
                                             int page,
                                             int size) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT, AdminRole.MODERATOR, AdminRole.FINANCE);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        SearchField field = SearchField.from(searchType);
        StatusFilter statusFilter = StatusFilter.from(accountStatus);

        String where = buildWhere(keyword, field, statusFilter);

        String selectJpql = """
                select new mannabom_server.manabom.application.admin.dto.response.AdminUserSummaryResponse(
                    u.userId,
                    p.profileId,
                    u.kakaoId,
                    u.userName,
                    p.nickName,
                    p.gender,
                    uni.name,
                    r.sidoName,
                    r.sigunguName,
                    u.isVerified,
                    case
                        when wallet.membershipActiveUntil is not null and wallet.membershipActiveUntil > current_timestamp
                        then true
                        else false
                    end,
                    restriction.status,
                    restriction.suspendedUntil,
                    u.createdAt,
                    :now
                )
                from User u
                left join Profile p on p.user = u
                left join p.university uni
                left join p.region r
                left join TingWallet wallet on wallet.userId = u.userId
                left join UserAccountRestriction restriction on restriction.userId = u.userId
                """ + where + " order by u.userId desc";

        TypedQuery<AdminUserSummaryResponse> query = entityManager.createQuery(selectJpql, AdminUserSummaryResponse.class);
        query.setParameter("now", LocalDateTime.now());
        applySearchParams(query, keyword, field, statusFilter);
        List<AdminUserSummaryResponse> users = query
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();

        String countJpql = """
                select count(u)
                from User u
                left join Profile p on p.user = u
                left join UserAccountRestriction restriction on restriction.userId = u.userId
                """ + where;
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        applySearchParams(countQuery, keyword, field, statusFilter);
        long totalCount = countQuery.getSingleResult();

        return AdminUserListResponse.builder()
                .users(users)
                .totalCount(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / safeSize))
                .page(safePage)
                .size(safeSize)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(AdminPrincipal admin, Long userId) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT, AdminRole.MODERATOR, AdminRole.FINANCE);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUser(user).orElse(null);
        TingWallet wallet = tingWalletRepository.findByUserId(userId).orElse(null);
        UserAccountRestriction restriction = userAccountRestrictionRepository.findById(userId).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        return AdminUserDetailResponse.builder()
                .userId(user.getUserId())
                .kakaoId(user.getKakaoId())
                .userName(user.getUserName())
                .verified(user.getIsVerified())
                .membership(wallet != null && wallet.isMembershipActive(LocalDateTime.now()))
                .accountStatus(effectiveStatus(restriction, now))
                .statusReason(restriction == null ? null : restriction.getReason())
                .statusSuspendedUntil(restriction == null ? null : restriction.getSuspendedUntil())
                .createdAt(user.getCreatedAt())
                .profile(toProfile(profile))
                .wallet(toWallet(wallet))
                .photos(toPhotos(profile))
                .build();
    }

    @Transactional
    public AdminUserDetailResponse updateUserStatus(AdminPrincipal admin,
                                                    Long userId,
                                                    AdminUpdateUserStatusRequest request,
                                                    String ipAddress) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.OPERATOR);
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        UserAccountRestriction restriction = userAccountRestrictionRepository.findById(userId)
                .orElseGet(() -> UserAccountRestriction.builder()
                        .userId(userId)
                        .status(UserAccountStatus.ACTIVE)
                        .updatedByAdminId(admin.adminId())
                        .build());
        validateSuspension(request.getStatus(), request.getSuspendedUntil());
        LocalDateTime auditNow = LocalDateTime.now();
        String before = accountStatusLabel(restriction, auditNow);
        restriction.update(request.getStatus(), request.getReason(), request.getSuspendedUntil(), admin.adminId());
        userAccountRestrictionRepository.save(restriction);

        String after = accountStatusLabel(restriction, auditNow);
        adminAuditService.log(admin.adminId(), AdminAuditActionType.USER_STATUS_UPDATE,
                AdminAuditTargetType.USER, userId, before, after, request.getReason(), ipAddress);
        return getUser(admin, userId);
    }

    private String buildWhere(String keyword, SearchField field, StatusFilter statusFilter) {
        List<String> conditions = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            conditions.add(searchCondition(field));
        }
        if (statusFilter != StatusFilter.ALL) {
            conditions.add(statusCondition(statusFilter));
        }
        if (conditions.isEmpty()) {
            return "";
        }
        return " where " + String.join(" and ", conditions);
    }

    private String searchCondition(SearchField field) {
        return switch (field) {
            case USER_ID -> "cast(u.userId as string) = :keyword";
            case PROFILE_ID -> "cast(p.profileId as string) = :keyword";
            case KAKAO_ID -> "lower(u.kakaoId) like :likeKeyword";
            case NICKNAME -> "lower(p.nickName) like :likeKeyword";
            case USER_NAME -> "lower(u.userName) like :likeKeyword";
            case ALL -> """
                    (cast(u.userId as string) = :keyword
                       or cast(p.profileId as string) = :keyword
                       or lower(u.kakaoId) like :likeKeyword
                       or lower(u.userName) like :likeKeyword
                       or lower(p.nickName) like :likeKeyword)
                    """;
        };
    }

    private String statusCondition(StatusFilter statusFilter) {
        return switch (statusFilter) {
            case ACTIVE -> """
                    (restriction is null
                       or restriction.status = :status
                       or (restriction.status = :suspendedStatus
                           and restriction.suspendedUntil is not null
                           and restriction.suspendedUntil <= current_timestamp))
                    """;
            case SUSPENDED -> """
                    restriction.status = :status
                       and (restriction.suspendedUntil is null or restriction.suspendedUntil > current_timestamp)
                    """;
            case WITHDRAWN -> "restriction.status = :status";
            case ALL -> "";
        };
    }

    private void applySearchParams(TypedQuery<?> query, String keyword, SearchField field, StatusFilter statusFilter) {
        if (!StringUtils.hasText(keyword)) {
            applyStatusParams(query, statusFilter);
            return;
        }
        if (field == SearchField.ALL || field == SearchField.USER_ID || field == SearchField.PROFILE_ID) {
            query.setParameter("keyword", keyword);
        }
        if (field == SearchField.ALL || field == SearchField.KAKAO_ID || field == SearchField.NICKNAME || field == SearchField.USER_NAME) {
            query.setParameter("likeKeyword", "%" + keyword.toLowerCase() + "%");
        }
        applyStatusParams(query, statusFilter);
    }

    private void applyStatusParams(TypedQuery<?> query, StatusFilter statusFilter) {
        if (statusFilter == StatusFilter.ALL) {
            return;
        }
        query.setParameter("status", UserAccountStatus.valueOf(statusFilter.name()));
        if (statusFilter == StatusFilter.ACTIVE) {
            query.setParameter("suspendedStatus", UserAccountStatus.SUSPENDED);
        }
    }

    private AdminUserDetailResponse.Profile toProfile(Profile profile) {
        if (profile == null) {
            return null;
        }
        return AdminUserDetailResponse.Profile.builder()
                .profileId(profile.getProfileId())
                .nickName(profile.getNickName())
                .gender(profile.getGender())
                .birthDate(profile.getBirthDate())
                .universityName(profile.getUniversity() == null ? null : profile.getUniversity().getName())
                .regionSidoName(profile.getRegion() == null ? null : profile.getRegion().getSidoName())
                .regionSigunguName(profile.getRegion() == null ? null : profile.getRegion().getSigunguName())
                .grade(profile.getGrade())
                .height(profile.getHeight())
                .build();
    }

    private AdminUserDetailResponse.Wallet toWallet(TingWallet wallet) {
        if (wallet == null) {
            return null;
        }
        return AdminUserDetailResponse.Wallet.builder()
                .ting(wallet.getTing())
                .eventTing(wallet.getEventTing())
                .membershipActiveUntil(wallet.getMembershipActiveUntil())
                .vipGrantedDate(wallet.getVipGrantedDate())
                .build();
    }

    private List<AdminUserDetailResponse.Photo> toPhotos(Profile profile) {
        if (profile == null) {
            return List.of();
        }
        return profileImageRepository.findByProfileOrderByImageIndex(profile).stream()
                .map(this::toPhoto)
                .toList();
    }

    private UserAccountStatus effectiveStatus(UserAccountRestriction restriction, LocalDateTime now) {
        if (restriction == null) {
            return UserAccountStatus.ACTIVE;
        }
        return restriction.effectiveStatus(now);
    }

    private void validateSuspension(UserAccountStatus status, LocalDateTime suspendedUntil) {
        if (status != UserAccountStatus.SUSPENDED) {
            return;
        }
        if (suspendedUntil == null || !suspendedUntil.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("정지 만료 시각은 현재 시각 이후여야 합니다.");
        }
    }

    private String accountStatusLabel(UserAccountRestriction restriction, LocalDateTime now) {
        if (restriction == null) {
            return UserAccountStatus.ACTIVE.name();
        }
        UserAccountStatus effectiveStatus = restriction.effectiveStatus(now);
        if (effectiveStatus == UserAccountStatus.SUSPENDED && restriction.getSuspendedUntil() != null) {
            return effectiveStatus.name() + " until " + restriction.getSuspendedUntil();
        }
        return effectiveStatus.name();
    }

    private AdminUserDetailResponse.Photo toPhoto(ProfileImage image) {
        String key = fileStoragePort.extractKeyFromUrl(image.getUrl());
        return AdminUserDetailResponse.Photo.builder()
                .imageId(image.getImageId())
                .url(fileStoragePort.presignedGetUrl(key, Duration.ofMinutes(10)))
                .imageIndex(image.getImageIndex())
                .main(image.getIsMain())
                .build();
    }

    private void requireAnyRole(AdminPrincipal admin, AdminRole... roles) {
        if (admin.hasAnyRole(roles)) {
            return;
        }
        throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
    }

    private enum SearchField {
        ALL,
        KAKAO_ID,
        NICKNAME,
        USER_ID,
        PROFILE_ID,
        USER_NAME;

        static SearchField from(String value) {
            try {
                return value == null ? ALL : SearchField.valueOf(value);
            } catch (IllegalArgumentException ex) {
                return ALL;
            }
        }
    }

    private enum StatusFilter {
        ALL,
        ACTIVE,
        SUSPENDED,
        WITHDRAWN;

        static StatusFilter from(String value) {
            try {
                return value == null ? ALL : StatusFilter.valueOf(value);
            } catch (IllegalArgumentException ex) {
                return ALL;
            }
        }
    }
}
