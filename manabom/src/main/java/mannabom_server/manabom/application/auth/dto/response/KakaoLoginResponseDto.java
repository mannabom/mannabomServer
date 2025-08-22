package mannabom_server.manabom.application.auth.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.auth.enums.UserStatus;
import mannabom_server.manabom.domain.user.enums.Gender;

/**
 * 카카오 로그인 응답 DTO
 */
@Getter
@Builder
public class KakaoLoginResponseDto {

    private boolean success;            // 요청 성공 여부
    private UserStatus userStatus;      // 사용자 상태 구분
    private KakaoLoginDataDto data;     // 상태별 데이터
    private String message;             // 결과 메시지

    /**
     * 기존 회원 로그인 성공 응답 생성
     */
    public static KakaoLoginResponseDto ofExistingUser(
            String accessToken, String refreshToken, Long userId, String nickname) {

        KakaoLoginDataDto data = KakaoLoginDataDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .nickname(nickname)
                .build();

        return KakaoLoginResponseDto.builder()
                .success(true)
                .userStatus(UserStatus.ACTIVE)
                .data(data)
                .message("로그인 성공")
                .build();
    }

    /**
     * 신규 사용자 응답 생성
     */
    public static KakaoLoginResponseDto ofNewUser(
            String kakaoId, String name, Integer birthYear, Gender gender, String profileId) {

        KakaoUserInfoDto kakaoUserInfo = KakaoUserInfoDto.builder()
                .kakaoId(kakaoId)
                .name(name)
                .birthYear(birthYear)
                .gender(gender)
                .profileId(profileId)
                .build();

        KakaoLoginDataDto data = KakaoLoginDataDto.builder()
                .kakaoUserInfo(kakaoUserInfo)
                .build();

        return KakaoLoginResponseDto.builder()
                .success(true)
                .userStatus(UserStatus.PENDING_VERIFICATION)
                .data(data)
                .message("회원가입이 필요합니다.")
                .build();
    }

    /**
     * 연령 제한 응답 생성
     */
    public static KakaoLoginResponseDto ofAgeRestricted(Integer birthYear) {
        KakaoLoginDataDto data = KakaoLoginDataDto.builder()
                .birthYear(birthYear)
                .build();

        return KakaoLoginResponseDto.builder()
                .success(false)
                .userStatus(UserStatus.AGE_RESTRICTED)
                .data(data)
                .message("20대만 이용 가능한 서비스입니다.")
                .build();
    }

    /**
     * 로그인 데이터 DTO
     */
    @Getter
    @Builder
    public static class KakaoLoginDataDto {

        // 기존 회원인 경우에만 제공
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String nickname;

        // 신규 사용자인 경우에만 제공
        private KakaoUserInfoDto kakaoUserInfo;

        // 연령 제한인 경우에만 제공
        private Integer birthYear;
    }

    /**
     * 카카오 사용자 정보 DTO
     */
    @Getter
    @Builder
    public static class KakaoUserInfoDto {
        private String kakaoId;     // 카카오 고유 ID
        private String name;        // 카카오에서 받은 실명
        private Integer birthYear;  // 출생년도
        private Gender gender;      // 성별
        private String profileId;   // 회원가입 진행용 프로필 ID
    }
}
