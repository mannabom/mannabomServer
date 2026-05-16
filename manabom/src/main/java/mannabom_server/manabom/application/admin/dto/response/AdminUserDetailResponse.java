package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.user.enums.Gender;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminUserDetailResponse {
    private final Long userId;
    private final String kakaoId;
    private final String userName;
    private final Boolean verified;
    private final Boolean membership;
    private final UserAccountStatus accountStatus;
    private final String statusReason;
    private final Instant createdAt;
    private final Profile profile;
    private final Wallet wallet;
    private final List<Photo> photos;

    @Getter
    @Builder
    public static class Profile {
        private final Long profileId;
        private final String nickName;
        private final Gender gender;
        private final LocalDate birthDate;
        private final String universityName;
        private final String regionSidoName;
        private final String regionSigunguName;
        private final Double grade;
        private final Integer height;
    }

    @Getter
    @Builder
    public static class Wallet {
        private final int ting;
        private final int eventTing;
        private final LocalDateTime membershipActiveUntil;
        private final LocalDate vipGrantedDate;
    }

    @Getter
    @Builder
    public static class Photo {
        private final Long imageId;
        private final String url;
        private final Integer imageIndex;
        private final Boolean main;
    }
}
