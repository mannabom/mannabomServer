package mannabom_server.manabom.application.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.auth.dto.request.KakaoLoginRequestDto;
import mannabom_server.manabom.application.auth.dto.request.RefreshTokenRequestDto;
import mannabom_server.manabom.application.auth.dto.response.KakaoLoginResponseDto;
import mannabom_server.manabom.application.auth.dto.response.RefreshTokenResponseDto;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.domain.auth.entity.RefreshToken;
import mannabom_server.manabom.domain.auth.repository.RefreshTokenRepository;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.deviceToken.DeviceToken;
import mannabom_server.manabom.domain.deviceToken.DeviceTokenRepository;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.signup.entity.SignupProgress;
import mannabom_server.manabom.domain.signup.repository.SignupProgressRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.external.kakao.KakaoApiService;
import mannabom_server.manabom.infrastructure.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 인증 관련 비즈니스 로직 - 카카오 정보를 SignupProgress에 저장하도록 수정
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final KakaoApiService kakaoApiService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SignupProgressRepository signupProgressRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final ProfileImageRepository profileImageRepository;
    private final JwtUtil jwtUtil;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FileStoragePort fileStoragePort;
    private final TingWalletRepository tingWalletRepository;

    // 사업자 등록 하기 전 임시 개발환경 전용 설정
    @Value("${app.kakao.development.skip-age-verification:false}")
    private boolean skipAgeVerification;

    @Value("${app.kakao.development.default-birth-year:2000}")
    private int defaultBirthYear;

    @Value("${app.kakao.development.default-gender:MALE}")
    private String defaultGender;

    /**
     * 카카오 로그인 처리 - 신규 사용자의 경우 SignupProgress에 카카오 정보 저장
     */
    public KakaoLoginResponseDto loginWithKakao(KakaoLoginRequestDto request) {
        log.info("카카오 로그인 처리 시작");

        try {
            // 1. request에서 access token 추출
            String accessToken = (String)request.getAccessToken();
            log.debug("카카오 access token 추출 완료");

            // 2. 카카오에서 사용자 정보 조회
            Map<String, Object> userInfo = kakaoApiService.getKakaoUserInfo(accessToken);
            KakaoLoginResponseDto.KakaoUserInfoDto kakaoUserInfo = parseKakaoUserInfo(userInfo);
            log.debug("카카오 사용자 정보 파싱 완료 - 카카오 ID: {}", kakaoUserInfo.getKakaoId());

            // 3. 20대 연령 검증
            if (!isValidAge(kakaoUserInfo.getBirthYear())) {
                log.warn("연령 제한 사용자 접근 시도 - 출생년도: {}", kakaoUserInfo.getBirthYear());
                return KakaoLoginResponseDto.ofAgeRestricted(kakaoUserInfo.getBirthYear());
            }

            // 4. 기존 사용자인지 확인
            Optional<User> existingUser = userRepository.findByKakaoId(kakaoUserInfo.getKakaoId());

            if (existingUser.isPresent()) {
                log.info("기존 사용자 로그인 처리 - 사용자 ID: {}", existingUser.get().getUserId());
                return handleExistingUserLogin(existingUser.get());
            } else {
                log.info("신규 사용자 감지 - 회원가입 진행");
                return handleNewUserSignup(kakaoUserInfo);
            }

        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 오류 발생", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 신규 사용자 회원가입 진행 - Redis에 카카오 정보 저장
     */
    private KakaoLoginResponseDto handleNewUserSignup(KakaoLoginResponseDto.KakaoUserInfoDto kakaoUserInfo) {
        String profileId = UUID.randomUUID().toString();

        SignupProgress signupProgress = SignupProgress.builder()
                .profileId(profileId)
                .kakaoId(kakaoUserInfo.getKakaoId())
                .userName(kakaoUserInfo.getName())
                .birthYear(kakaoUserInfo.getBirthYear())
                .gender(kakaoUserInfo.getGender().name())
                .currentStep(1)
                .build();

        signupProgressRepository.save(signupProgress);
        log.info("신규 회원가입 진행 상태 생성 완료 - Profile ID: {}, 카카오 정보 저장됨", profileId);

        return KakaoLoginResponseDto.ofNewUser(
                kakaoUserInfo.getKakaoId(),
                kakaoUserInfo.getName(),
                kakaoUserInfo.getBirthYear(),
                kakaoUserInfo.getGender(),
                profileId
        );
    }

    /**
     * 카카오 사용자 정보 파싱
     */
    private KakaoLoginResponseDto.KakaoUserInfoDto parseKakaoUserInfo(Map<String, Object> userInfo) {
        log.debug("카카오 사용자 정보 파싱 시작");

        try {
            // 카카오 ID 추출
            String kakaoId = String.valueOf(userInfo.get("id"));

            // 카카오 계정 정보 추출
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            if (kakaoAccount == null) {
                throw new IllegalArgumentException("카카오 계정 정보를 찾을 수 없습니다.");
            }

            // 프로필 정보 추출
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            String nickname = null;
            if (profile != null) {
                nickname = (String) profile.get("nickname");
            }

            // 생년월일 정보 추출
            String birthyear = (String) kakaoAccount.get("birthyear");
            Integer birthYear = null;

            if (birthyear != null && !birthyear.isEmpty()) {
                birthYear = Integer.parseInt(birthyear);
            } else if (skipAgeVerification) {
                birthYear = defaultBirthYear;
                log.debug("개발환경: 더미 출생년도 사용 - {}", birthYear);
            }

            // 성별 정보 추출
            String genderString = (String) kakaoAccount.get("gender");
            Gender gender = null;

            if (genderString != null) {
                gender = "male".equals(genderString) ? Gender.MALE : Gender.FEMALE;
            } else if (skipAgeVerification) {
                gender = Gender.valueOf(defaultGender);
                log.debug("개발환경: 더미 성별 사용 - {}", gender);
            }

            log.debug("카카오 사용자 정보 파싱 완료 - 카카오 ID: {}, 이름: {}, 출생년도: {}, 성별: {}",
                    kakaoId, nickname, birthYear, gender);

            return KakaoLoginResponseDto.KakaoUserInfoDto.builder()
                    .kakaoId(kakaoId)
                    .name(nickname != null ? nickname : "사용자")
                    .birthYear(birthYear)
                    .gender(gender)
                    .build();

        } catch (Exception e) {
            log.error("카카오 사용자 정보 파싱 실패", e);
            throw new IllegalArgumentException("카카오 사용자 정보를 파싱할 수 없습니다.", e);
        }
    }

    /**
     * 20대 연령 검증 (20~29세)
     */
    private boolean isValidAge(Integer birthYear) {
        if (birthYear == null) {
            log.warn("출생년도 정보가 없어 연령 검증 실패");
            return false;
        }

        int currentYear = LocalDate.now().getYear();
        int age = currentYear - birthYear;
        boolean isValid = age >= 20 && age <= 29;

        if (skipAgeVerification && !isValid) {
            log.debug("임시환경: 연령 제한 무시 - 출생년도: {}, 나이: {}", birthYear, age);
            return true;
        }

        log.debug("연령 검증 결과 - 출생년도: {}, 나이: {}, 유효성: {}", birthYear, age, isValid);
        return isValid;
    }

    /**
     * 토큰 갱신 처리
     */
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        log.info("토큰 갱신 요청 처리 시작");

        // 1. refresh token 검증
        RefreshToken refreshTokenEntity = refreshTokenRepository
                .findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("유효하지 않은 refresh token 으로 갱신 시도");
                    return new IllegalArgumentException("유효하지 않은 refresh token 입니다.");
                });

        // 2. 토큰 만료 확인
        if (refreshTokenEntity.isExpired()) {
            log.warn("만료된 refresh token 으로 갱신 시도");
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new IllegalArgumentException("만료된 refresh token 입니다.");
        }

        // 3. 새로운 토큰들 생성
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user.getUserId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        // 4. refresh token 업데이트
        LocalDateTime newExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000);
        refreshTokenEntity.updateToken(newRefreshToken, newExpiresAt);

        log.info("토큰 갱신 완료 - 사용자 ID: {}", user.getUserId());

        return RefreshTokenResponseDto.of(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpiration() / 1000
        );
    }

    /**
     * 기존 사용자 로그인 처리
     */
    private KakaoLoginResponseDto handleExistingUserLogin(User user) {
        log.debug("기존 사용자 JWT 토큰 생성 시작 - 사용자 ID: {}", user.getUserId());

        // 1. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        // 2. refresh token 저장/업데이트
        saveOrUpdateRefreshToken(user, refreshToken);

        // 3. 닉네임 조회 (프로필이 있는 경우)
        String nickname = profileRepository.findByUser(user)
                .map(Profile::getNickName)
                .orElse(user.getUserName());

        log.debug("기존 사용자 로그인 처리 완료 - 닉네임: {}", nickname);

        return KakaoLoginResponseDto.ofExistingUser(
                accessToken,
                refreshToken,
                user.getUserId(),
                nickname
        );
    }

    /**
     * refresh token 저장 또는 업데이트
     */
    private void saveOrUpdateRefreshToken(User user, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            // 기존 토큰 업데이트
            existingToken.get().updateToken(refreshToken, expiresAt);
            log.debug("기존 refresh token 업데이트 완료");
        } else {
            // 새 토큰 저장
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();
            refreshTokenRepository.save(newRefreshToken);
            log.debug("새 refresh token 저장 완료");
        }
    }

    /**
     * 로그아웃 처리
     */
    public void logout(Long userId, String token) {
        log.info("로그아웃 요청 처리 - 사용자 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        refreshTokenRepository.deleteByUser(user);

        DeviceToken deviceToken = deviceTokenRepository.findByToken(token).orElseThrow(()->new IllegalArgumentException("해당 토큰이 존재하지 않습니다!"));
        if(!Objects.equals(deviceToken.getUserId(), userId)) {
            throw new IllegalArgumentException("본인 소유의 토큰만 로그아웃 처리할 수 있습니다.");
        }

        deviceToken.deactivate();
        log.info("deviceToken 비활성화 완료 - userId={}, tokenId={}", userId, deviceToken.getId());

        log.info("로그아웃 완료 - 사용자 ID: {}", userId);
    }

    /**
     * 회원 탈퇴 처리
     */
    @Transactional
    public void deleteUser(Long userId){
        log.info("회원 탈퇴 요청 처리 - 사용자 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Profile profile = profileRepository.findByUser(user).orElse(null);

        Optional<TingWallet> tingWalletOpt = tingWalletRepository.findById(userId);

        List<String> storageUrlsToDelete = new ArrayList<>();

        if(profile != null){
            List<ProfileImage> images = profileImageRepository.findAllByProfile(profile);

            for(ProfileImage img : images){
                String storageUrl = img.getUrl();
                if(storageUrl != null && !storageUrl.isBlank()){
                    storageUrlsToDelete.add(storageUrl);
                }
            }

            questionAnswerRepository.deleteByProfile(profile);
            profileImageRepository.deleteByProfile(profile);
            profileRepository.delete(profile);
        }

        tingWalletOpt.ifPresentOrElse(
                tingWalletRepository::delete,
                () -> log.warn("회원 탈퇴 중 지갑이 존재하지 않습니다. userId : {}", userId)
        );

        if(!storageUrlsToDelete.isEmpty()) {
            if(TransactionSynchronizationManager.isSynchronizationActive()){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit(){
                        for(String url : storageUrlsToDelete){
                            try{
                                fileStoragePort.deleteFile(url);
                                log.info("스토리지 이미지 삭제 완료, url : {}", url);
                            } catch (Exception e){
                                log.error("스토리지 이미지 삭제 실패, url : {}", url, e);
                            }
                        }
                    }
                });
            } else {
                log.warn("트랜잭션 동기화 비활성 - 스토리지 즉시 삭제로 처리");
                for(String url : storageUrlsToDelete){
                    try{
                        fileStoragePort.deleteFile(url);
                        log.info("스토리지 이미지 삭제 완료, url : {}", url);
                    } catch (Exception e){
                        log.error("스토리지 이미지 삭제 실패, url : {}", url, e);
                    }
                }
            }
        }

        refreshTokenRepository.deleteByUser(user);

        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);
        for(DeviceToken token : deviceTokens){
            token.deactivate();
        }
        log.info("해당 사용자의 모든 deviceToken 비활성화 완료");

        userRepository.deleteById(userId);

        log.info("회원 탈퇴 완료 - 사용자 ID: {}", userId);
    }

}
