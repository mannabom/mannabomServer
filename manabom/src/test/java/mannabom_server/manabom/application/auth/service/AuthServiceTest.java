package mannabom_server.manabom.application.auth.service;

import mannabom_server.manabom.application.auth.dto.request.KakaoLoginRequestDto;
import mannabom_server.manabom.application.auth.dto.response.KakaoLoginResponseDto;
import mannabom_server.manabom.application.auth.exception.KakaoConsentRequiredException;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.domain.auth.enums.UserStatus;
import mannabom_server.manabom.domain.auth.repository.RefreshTokenRepository;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.deviceToken.DeviceTokenRepository;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.signup.entity.SignupProgress;
import mannabom_server.manabom.domain.signup.repository.SignupProgressRepository;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.external.kakao.KakaoApiService;
import mannabom_server.manabom.infrastructure.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private KakaoApiService kakaoApiService;
    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SignupProgressRepository signupProgressRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private ProfileImageRepository profileImageRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private TingWalletRepository tingWalletRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void savesKakaoAccountInformationForNewUser() {
        when(kakaoApiService.getKakaoUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("홍길동", "2001", "male"));
        when(userRepository.findByKakaoId("98765")).thenReturn(Optional.empty());

        KakaoLoginResponseDto response = authService.loginWithKakao(
                new KakaoLoginRequestDto("kakao-access-token")
        );

        ArgumentCaptor<SignupProgress> progressCaptor = ArgumentCaptor.forClass(SignupProgress.class);
        verify(signupProgressRepository).save(progressCaptor.capture());
        SignupProgress savedProgress = progressCaptor.getValue();

        assertThat(response.getUserStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(response.getData().getKakaoUserInfo().getName()).isEqualTo("홍길동");
        assertThat(response.getData().getKakaoUserInfo().getBirthYear()).isEqualTo(2001);
        assertThat(response.getData().getKakaoUserInfo().getGender()).isEqualTo(Gender.MALE);
        assertThat(savedProgress.getKakaoId()).isEqualTo("98765");
        assertThat(savedProgress.getUserName()).isEqualTo("홍길동");
        assertThat(savedProgress.getBirthYear()).isEqualTo(2001);
        assertThat(savedProgress.getGender()).isEqualTo("MALE");
    }

    @Test
    void rejectsLoginWhenRequiredKakaoInformationIsMissing() {
        Map<String, Object> userInfo = Map.of(
                "id", 98765L,
                "kakao_account", Map.of("name", "홍길동")
        );
        when(kakaoApiService.getKakaoUserInfo("kakao-access-token")).thenReturn(userInfo);

        assertThatThrownBy(() -> authService.loginWithKakao(
                new KakaoLoginRequestDto("kakao-access-token")
        ))
                .isInstanceOf(KakaoConsentRequiredException.class)
                .satisfies(exception -> assertThat(
                        ((KakaoConsentRequiredException) exception).getRequiredScopes()
                ).containsExactly("birthyear", "gender"));

        verify(signupProgressRepository, never()).save(any(SignupProgress.class));
        verify(userRepository, never()).findByKakaoId(any());
    }

    @Test
    void returnsAgeRestrictedWithoutCreatingSignupProgress() {
        when(kakaoApiService.getKakaoUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("홍길동", "1980", "female"));

        KakaoLoginResponseDto response = authService.loginWithKakao(
                new KakaoLoginRequestDto("kakao-access-token")
        );

        assertThat(response.getUserStatus()).isEqualTo(UserStatus.AGE_RESTRICTED);
        assertThat(response.getData().getBirthYear()).isEqualTo(1980);
        verify(signupProgressRepository, never()).save(any(SignupProgress.class));
        verify(userRepository, never()).findByKakaoId(any());
    }

    private Map<String, Object> kakaoUserInfo(String name, String birthyear, String gender) {
        return Map.of(
                "id", 98765L,
                "kakao_account", Map.of(
                        "name", name,
                        "birthyear", birthyear,
                        "gender", gender
                )
        );
    }
}
