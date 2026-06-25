package mannabom_server.manabom.application.admin.dto.response;

import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.user.enums.Gender;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
public class AdminUserSummaryResponse {
    private Long userId;
    private Long profileId;
    private String kakaoId;
    private String userName;
    private String nickName;
    private Gender gender;
    private String universityName;
    private String regionSidoName;
    private String regionSigunguName;
    private Boolean verified;
    private Boolean membership;
    private UserAccountStatus accountStatus;
    private LocalDateTime statusSuspendedUntil;
    private Instant createdAt;

    public AdminUserSummaryResponse(Long userId,
                                    Long profileId,
                                    String kakaoId,
                                    String userName,
                                    String nickName,
                                    Gender gender,
                                    String universityName,
                                    String regionSidoName,
                                    String regionSigunguName,
                                    Boolean verified,
                                    Boolean membership,
                                    UserAccountStatus accountStatus,
                                    LocalDateTime statusSuspendedUntil,
                                    Instant createdAt) {
        this.userId = userId;
        this.profileId = profileId;
        this.kakaoId = kakaoId;
        this.userName = userName;
        this.nickName = nickName;
        this.gender = gender;
        this.universityName = universityName;
        this.regionSidoName = regionSidoName;
        this.regionSigunguName = regionSigunguName;
        this.verified = verified;
        this.membership = membership;
        this.accountStatus = effectiveStatus(accountStatus, statusSuspendedUntil);
        this.statusSuspendedUntil = statusSuspendedUntil;
        this.createdAt = createdAt;
    }

    private UserAccountStatus effectiveStatus(UserAccountStatus status, LocalDateTime suspendedUntil) {
        if (status == null) {
            return UserAccountStatus.ACTIVE;
        }
        if (status == UserAccountStatus.SUSPENDED
                && suspendedUntil != null
                && !suspendedUntil.isAfter(LocalDateTime.now())) {
            return UserAccountStatus.ACTIVE;
        }
        return status;
    }
}
