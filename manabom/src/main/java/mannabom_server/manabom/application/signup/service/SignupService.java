package mannabom_server.manabom.application.signup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.signup.dto.request.*;
import mannabom_server.manabom.application.signup.dto.response.*;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.domain.auth.entity.RefreshToken;
import mannabom_server.manabom.domain.auth.repository.RefreshTokenRepository;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.question.repository.QuestionRepository;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.region.repository.RegionRepository;
import mannabom_server.manabom.domain.signup.entity.SignupProgress;
import mannabom_server.manabom.domain.signup.repository.SignupProgressRepository;
import mannabom_server.manabom.domain.university.entity.University;
import mannabom_server.manabom.domain.university.repository.UniversityRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.*;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.security.jwt.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 회원가입 메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupService {

    private final SignupProgressRepository signupProgressRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UniversityRepository universityRepository;
    private final RegionRepository regionRepository;

    private final EmailService emailService;
    private final FileStoragePort fileStoragePort;
    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;
    private final TingWalletRepository tingWalletRepository;
    private final TingTransactionRecorder tingTransactionRecorder;

    private static final int BONUS_OPTIONAL_TEXT_MALE = 11;
    private static final int BONUS_OPTIONAL_TEXT_FEMALE = 6;
    private static final int BONUS_REQUIRED_CHOICE_MALE = 2;
    private static final int BONUS_REQUIRED_CHOICE_FEMALE = 2;

    /**
     * 1단계: 기본 프로필 정보 저장
     */
    public ProfileRelationshipResponseDto saveProfileRelationship(ProfileRelationshipRequestDto request) {
        log.info("기본 프로필 정보 저장 시작 - Profile ID: {}", request.getProfileId());

        // 1. 카카오 로그인 후 생성된 프로필 아이디로 조회
        SignupProgress progress = signupProgressRepository.findById(request.getProfileId())
                .orElseThrow(() -> new IllegalArgumentException("회원가입 진행 정보를 찾을 수 없습니다. 다시 로그인해주세요."));

        // 2. 기본 정보 업데이트
        progress.updateBasicInfo(
                request.getHeight(),
                request.getBodyType() != null ? request.getBodyType().name() : null,
                request.getRegion().getSido(),
                request.getRegion().getSigungu(),
                request.getMbti(),
                request.getSmokingHabit() != null ? request.getSmokingHabit().name() : null,
                request.getDrinkingHabit() != null ? request.getDrinkingHabit().name() : null
        );

        // 3. 필수 질문 답변 저장
        if (request.getSelfIntroduction() != null) {
            progress.addQuestionAnswer("self_introduction", request.getSelfIntroduction());
        }
        if (request.getAttractivePartnerTrait() != null) {
            progress.addQuestionAnswer("attractive_partner_trait", request.getAttractivePartnerTrait());
        }
        if (request.getDesiredPartnerTrait() != null) {
            progress.addQuestionAnswer("desired_partner_trait", request.getDesiredPartnerTrait());
        }

        // 4. 선택 질문 답변 저장
        if (request.getOptionalAnswers() != null) {
            request.getOptionalAnswers().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    progress.addQuestionAnswer(key, value);
                }
            });
        }

        // 5. 연애관 이지선다 답변 저장
        if (request.getRelationshipChoices() != null) {
            request.getRelationshipChoices().forEach((key, value) -> {
                if (value != null) {
                    progress.addQuestionAnswer("relationship_" + key, value);
                }
            });
        }

        // 6. Redis에 저장
        signupProgressRepository.save(progress);

        log.info("기본 프로필 정보 저장 완료 - Profile ID: {}", progress.getProfileId());

        return ProfileRelationshipResponseDto.builder()
                .success(true)
                .data(ProfileRelationshipResponseDto.ProfileRelationshipDataDto.builder()
                        .profileId(progress.getProfileId())
                        .build())
                .message("프로필 정보가 저장되었습니다.")
                .build();
    }

    /**
     * 2단계: 닉네임 중복 확인 (Redis 검사 추가)
     */
    @Transactional(readOnly = true)
    public NicknameCheckResponseDto checkNickname(NicknameCheckRequestDto request) {
        log.info("닉네임 중복 확인 - 닉네임: {}", request.getNickname());

        // DB + Redis 중복 확인
        boolean dbExists = profileRepository.existsByNickName(request.getNickname());
        boolean redisExists = dbExists ? false : checkNicknameInRedis(request.getNickname());
        boolean available = !dbExists && !redisExists;

        return NicknameCheckResponseDto.builder()
                .success(true)
                .data(NicknameCheckResponseDto.NicknameCheckDataDto.builder()
                        .available(available)
                        .build())
                .message(available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.")
                .build();
    }


    /**
     * 2단계: 닉네임 설정
     */
    public SetNicknameResponseDto setNickname(SetNicknameRequestDto request) {
        log.info("닉네임 설정 시작 - Profile ID: {}, 닉네임: {}", request.getProfileId(), request.getNickname());

        // 1. 진행상태 조회
        SignupProgress progress = getSignupProgress(request.getProfileId());

        // 2. 실제 DB에서 중복 확인
        boolean dbExists = profileRepository.existsByNickName(request.getNickname());

        if (dbExists) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3. 닉네임 업데이트
        progress.updateNickname(request.getNickname(), true);
        signupProgressRepository.save(progress);

        log.info("닉네임 설정 완료 - Profile ID: {}", request.getProfileId());

        return SetNicknameResponseDto.builder()
                .success(true)
                .data(SetNicknameResponseDto.SetNicknameDataDto.builder()
                        .profileId(request.getProfileId())
                        .build())
                .message("닉네임이 설정되었습니다.")
                .build();
    }

    /**
     * 3단계: 대학 이메일 인증번호 발송
     */
    public SendEmailVerificationResponseDto sendEmailVerification(SendEmailVerificationRequestDto request) {
        log.info("이메일 인증번호 발송 - Profile ID: {}, 이메일: {}", request.getProfileId(), request.getEmail());

        // 1. 진행상태 조회
        SignupProgress progress = getSignupProgress(request.getProfileId());

        // 2. 대학 도메인 검증
        String domain = extractDomain(request.getEmail());
        University university = universityRepository.findByDomain(domain)
                .orElseThrow(()->new IllegalArgumentException("지원하지 않는 대학 이메일입니다."));

        // 3. 실제 DB에서 이메일 중복 확인
        boolean dbExists = profileRepository.findByEmail(request.getEmail()).isPresent();

        if (dbExists) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 4. 인증번호 발송
        String verificationCode = emailService.sendVerificationCode(request.getEmail());

        // 5. 진행상태 업데이트
        progress.updateEmail(request.getEmail(), verificationCode, university.getName());
        signupProgressRepository.save(progress);

        log.info("이메일 인증번호 발송 완료");

        return SendEmailVerificationResponseDto.builder()
                .success(true)
                .data(SendEmailVerificationResponseDto.SendEmailVerificationDataDto.builder()
                        .emailSent(true)
                        .build())
                .message("인증번호가 발송되었습니다.")
                .build();
    }

    /**
     * 3단계: 이메일 인증번호 확인
     */
    public VerifyEmailResponseDto verifyEmail(VerifyEmailRequestDto request) {
        log.info("이메일 인증번호 확인 - Profile ID: {}", request.getProfileId());

        // 1. 진행상태 조회
        SignupProgress progress = getSignupProgress(request.getProfileId());

        // 2. 인증번호 확인
        boolean isValid = emailService.verifyCode(progress.getEmail(), request.getVerificationCode());

        if (!isValid) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 3. 인증 완료 처리
        progress.markEmailVerified();

        signupProgressRepository.save(progress);
        log.info("이메일 인증 완료");

        return VerifyEmailResponseDto.builder()
                .success(true)
                .data(VerifyEmailResponseDto.VerifyEmailDataDto.builder()
                        .verified(true)
                        .build())
                .message("이메일 인증이 완료되었습니다.")
                .build();
    }

    /**
     * 4단계: 프로필 사진 업로드
     */
    public ProfilePhotosResponseDto uploadProfilePhotos(ProfilePhotosRequestDto request) {
        log.info("프로필 사진 업로드 - Profile ID: {}, 사진 개수: {}",
                request.getProfileId(), request.getPhotos().size());

        // 1. 진행상태 조회
        SignupProgress progress = getSignupProgress(request.getProfileId());

        // 2. 파일 업로드 및 URL 생성
        List<ProfilePhotosResponseDto.UploadedPhotoDto> uploadedPhotos = new ArrayList<>();

        for (int i = 0; i < request.getPhotos().size(); i++) {
            MultipartFile photo = request.getPhotos().get(i);

            String fileUrl = fileStoragePort.uploadFile(photo, "profiles");
            String fileKey = fileStoragePort.extractKeyFromUrl(fileUrl);

            // Redis에 저장
            progress.addProfileImage(String.valueOf(i), fileUrl);

            String presignedUrl = fileStoragePort.presignedGetUrl(fileKey, Duration.ofMinutes(10));

            uploadedPhotos.add(ProfilePhotosResponseDto.UploadedPhotoDto.builder()
                    .photoId(UUID.randomUUID().toString())
                    .url(presignedUrl)
                    .build());
        }

        // 3. 진행상태 저장
        signupProgressRepository.save(progress);

        log.info("프로필 사진 업로드 완료");

        return ProfilePhotosResponseDto.builder()
                .success(true)
                .data(ProfilePhotosResponseDto.ProfilePhotosDataDto.builder()
                        .uploadedPhotos(uploadedPhotos)
                        .build())
                .message("프로필 사진이 업로드되었습니다.")
                .build();
    }

    /**
     * 5단계: 약관 동의
     */
    public TermsAgreementResponseDto agreeToTerms(TermsAgreementRequestDto request) {
        log.info("약관 동의 처리 - Profile ID: {}", request.getProfileId());

        // 1. 진행상태 조회
        SignupProgress progress = getSignupProgress(request.getProfileId());

        // 2. 필수 약관 동의 확인
        if (!request.getTermsAgreement().getServiceTerms() ||
                !request.getTermsAgreement().getPrivacyPolicy()) {
            throw new IllegalArgumentException("필수 약관에 동의해야 합니다.");
        }

        // 3. 약관 동의 정보 업데이트
        progress.updateTermsAgreement(
                request.getTermsAgreement().getServiceTerms(),
                request.getTermsAgreement().getPrivacyPolicy(),
                request.getTermsAgreement().getMarketingConsent()
        );

        signupProgressRepository.save(progress);

        log.info("약관 동의 완료");

        return TermsAgreementResponseDto.builder()
                .success(true)
                .data(TermsAgreementResponseDto.TermsAgreementDataDto.builder()
                        .agreed(true)
                        .build())
                .message("약관 동의가 완료되었습니다.")
                .build();
    }

    /**
     * 6단계: 회원가입 완료 - 카카오 정보가 이미 SignupProgress에 저장되어 있음
     */
    @Transactional
    public SignupCompleteResponseDto completeSignup(SignupCompleteRequestDto request) {
        log.info("회원가입 완료 처리 시작 - Profile ID: {}", request.getProfileId());

        // 1. 검증 (트랜잭션 없이)
        SignupProgress progress = validateSignupProgress(request.getProfileId());

        // 2. 핵심 DB 작업 (짧은 트랜잭션)
        User user = createUserWithProfile(progress);

        // 3. 초기 포인트 계산
        int signupBonusEventTing = calculateSignupBonusEventTing(progress.getQuestionAnswers(), Gender.valueOf(progress.getGender()));

        // 4. 토큰 생성 (외부 라이브러리 - 트랜잭션 없이)
        TokenPair tokens = generateTokens(user.getUserId());

        // 5. 부가 작업들 (별도 트랜잭션)
        saveRefreshToken(user, tokens.getRefreshToken());

        // 6. 정리 작업 (트랜잭션 없이)
        cleanupSignupProgress(progress);

        log.info("회원가입 완료 - 사용자 ID: {}, 초기 포인트: {}", user.getUserId(), signupBonusEventTing);

        return buildSignupCompleteResponse(user, tokens, signupBonusEventTing);
    }

    /**
     * 회원가입 진행 상태 검증 (트랜잭션 없이)
     */
    private SignupProgress validateSignupProgress(String profileId) {
        SignupProgress progress = getSignupProgress(profileId);

        if (!progress.isAllStepsCompleted()) {
            throw new IllegalArgumentException("모든 회원가입 단계를 완료해야 합니다.");
        }

        if (progress.getUserName() == null || progress.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이름 정보가 없습니다. 다시 로그인해주세요.");
        }

        if (progress.getBirthYear() == null) {
            throw new IllegalArgumentException("생년월일 정보가 없습니다. 다시 로그인해주세요.");
        }

        if (progress.getGender() == null || progress.getGender().trim().isEmpty()) {
            throw new IllegalArgumentException("성별 정보가 없습니다. 다시 로그인해주세요.");
        }

        log.info("회원가입 진행 상태 검증 완료 - 카카오 정보 포함 모든 단계 완료");
        return progress;
    }

    /**
     * 핵심 DB 작업
     */
    @Transactional
    protected User createUserWithProfile(SignupProgress progress) {
        log.debug("핵심 DB 작업 시작 - 카카오 ID: {}, 사용자명: {}",
                progress.getKakaoId(), progress.getUserName());

        // 1. User 생성 (카카오 정보 포함)
        User user = User.builder()
                .kakaoId(progress.getKakaoId())
                .userName(progress.getUserName())
                .build();
        user.verifyEmail();
        user = userRepository.save(user);

        // 2. TingWallet 생성 및 초기 지원금 지급
        //int initialPoints = calculateInitialPoints(progress.getGender()); (기본 지급을 답변 보상으로 대체)
        int signupBonusEventTing = calculateSignupBonusEventTing(progress.getQuestionAnswers(), Gender.valueOf(progress.getGender()));
        if(!tingWalletRepository.existsById(user.getUserId())) {
            TingWallet wallet = new TingWallet(user.getUserId());
            //wallet.addEventTing(initialPoints); (기본 지급을 답변 보상으로 대체)
            wallet.addEventTing(signupBonusEventTing);
            tingWalletRepository.save(wallet);
            tingTransactionRecorder.recordEvent(
                    wallet,
                    TingTransactionType.SIGNUP_BONUS,
                    signupBonusEventTing,
                    TingTransactionReferenceType.USER,
                    String.valueOf(user.getUserId()),
                    "SIGNUP:" + user.getUserId() + ":BONUS",
                    "회원가입 프로필 작성 보너스"
            );
            int savedEventTing = wallet.getEventTing();
            log.info("해당 유저 팅 지갑 생성 및 보너스 팅 지급 완료, 지급된 이벤트 팅 : {}", savedEventTing);
        }else
            log.error("이미 팅 지갑이 존재하는 유저( ID: {})", user.getUserId());

        // 3. Profile 생성
        Profile profile = createProfileFromProgress(user, progress);
        profile = profileRepository.save(profile);

        // 4. 관련 데이터들 (같은 트랜잭션에서 처리해야 함)
        saveProfileImages(profile, progress.getProfileImages());
        saveQuestionAnswers(profile, progress.getQuestionAnswers());

        log.debug("핵심 DB 작업 완료 - 사용자 ID: {}, 사용자명: {}",
                user.getUserId(), user.getUserName());
        return user;
    }

    private int calculateSignupBonusEventTing(Map<String, String> answers, Gender gender) {
        if(answers == null || answers.isEmpty())
            return 0;

        // 답변이 존재하는 질문들 id 추출
        Set<Long> questionIds = new HashSet<>();

        for(Map.Entry<String, String> e : answers.entrySet()){
            String answer = e.getValue();
            if(answer == null || answer.trim().isEmpty())
                continue;

            Long questionId = mapQuestionKeyToId(e.getKey());
            if(questionId != null){
                questionIds.add(questionId);
            }
        }

        if(questionIds.isEmpty())
            return 0;

        // 해당 id들로 Question 객체들 불러와서 Mapping 시키기
        List<Question> questions = questionRepository.findAllById(questionIds);

        Map<Long, Question> questionMap = new HashMap<>();
        for(Question q : questions){
            questionMap.put(q.getQuestionId(), q);
        }

        int bonus = 0;
        int optionalTextCount = 0;
        int requiredChoiceCount = 0;

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String answer = entry.getValue();
            if(answer == null || answer.trim().isEmpty())
                continue;

            Long questionId = mapQuestionKeyToId(entry.getKey());
            if(questionId == null)
                continue;

            Question question = questionMap.get(questionId);
            if(question == null)
                continue;

            QuestionType type = question.getQuestionType();

            if(type == QuestionType.OPTIONAL_TEXT){
                if(gender.equals(Gender.MALE))
                    bonus += BONUS_OPTIONAL_TEXT_MALE;
                else if(gender.equals(Gender.FEMALE))
                    bonus += BONUS_OPTIONAL_TEXT_FEMALE;
                else
                    throw new IllegalStateException("사용자의 성별 확인 불가(보너스 팅 지급 부분)");
                optionalTextCount++;
            }else if(type == QuestionType.REQUIRED_CHOICE){
                if(gender.equals(Gender.MALE))
                    bonus += BONUS_REQUIRED_CHOICE_MALE;
                else if(gender.equals(Gender.FEMALE))
                    bonus += BONUS_REQUIRED_CHOICE_FEMALE;
                else
                    throw new IllegalStateException("사용자의 성별 확인 불가(보너스 팅 지급 부분)");
                requiredChoiceCount++;
            }
        }
        log.info("선택 주관식 답변 완료 : {}, 필수 객관식 답변 완료 : {}, 총 지급 보너스 팅 : {}(성별 : {})", optionalTextCount, requiredChoiceCount, bonus, gender.equals(Gender.FEMALE)? "여성" : "남성");

        return bonus;
    }

    /**
     * JWT 토큰 생성 (외부 라이브러리 - 트랜잭션 불필요)
     */
    private TokenPair generateTokens(Long userId) {
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * RefreshToken 저장 (별도 트랜잭션)
     */
    @Transactional
    protected void saveRefreshToken(User user, String refreshToken) {
        try {
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000);

            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();

            refreshTokenRepository.save(refreshTokenEntity);
            log.debug("RefreshToken 저장 완료 - 사용자 ID: {}", user.getUserId());

        } catch (Exception e) {
            log.error("RefreshToken 저장 실패 - 사용자 ID: {} (가입은 완료됨)", user.getUserId(), e);
        }
    }

    /**
     * Redis에서 닉네임 중복 확인
     */
    private boolean checkNicknameInRedis(String nickname) {
        try {
            Iterable<SignupProgress> allProgress = signupProgressRepository.findAll();

            for (SignupProgress progress : allProgress) {
                if (progress.getNickName() != null &&
                        progress.getNickName().equals(nickname)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Redis 닉네임 중복 검사 실패 - DB 결과만 사용", e);
            return false;
        }
    }

    /**
     * 정리 작업 (Redis 데이터 삭제)
     */
    private void cleanupSignupProgress(SignupProgress progress) {
        try {
            signupProgressRepository.delete(progress);
            log.debug("회원가입 진행 상태 정리 완료");
        } catch (Exception e) {
            log.warn("회원가입 진행 상태 정리 실패 (서비스에는 영향 없음)", e);
        }
    }

    /**
     * 초기 포인트 계산
     */
    private int calculateInitialPoints(String gender) {
        return Gender.valueOf(gender) == Gender.MALE ? 60 : 30;
    }

    /**
     * 응답 생성
     */
    private SignupCompleteResponseDto buildSignupCompleteResponse(User user, TokenPair tokens, int initialPoints) {
        return SignupCompleteResponseDto.builder()
                .success(true)
                .data(SignupCompleteResponseDto.SignupCompleteDataDto.builder()
                        .userId(user.getUserId())
                        .accessToken(tokens.getAccessToken())
                        .refreshToken(tokens.getRefreshToken())
                        .initialPoints(initialPoints)
                        .build())
                .message("회원가입이 완료되었습니다.")
                .build();
    }

    private SignupProgress getSignupProgress(String profileId) {
        return signupProgressRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("회원가입 진행 정보를 찾을 수 없습니다."));
    }

    /**
     * 도메인 형식 추출
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다.");
        }
        return email.substring(atIndex + 1);
    }

    /**
     * 입력받은 회원 정보들 바탕으로 프로필 생성
     */
    private Profile createProfileFromProgress(User user, SignupProgress progress) {
        University university = universityRepository.findByName(progress.getUniversity())
                        .orElseThrow(()-> new IllegalArgumentException("유효하지 않은 대학입니다." + progress.getUniversity()));
        Region region = regionRepository.findBySidoNameAndSigunguName(progress.getRegionSido(),progress.getRegionSigungu())
                        .orElseThrow(()-> new IllegalArgumentException("유효하지 않은 지역입니다."));
        return Profile.builder()
                .user(user)
                .gender(Gender.valueOf(progress.getGender()))
                .height(progress.getHeight())
                .bodyType(progress.getBodyType() != null ? BodyType.valueOf(progress.getBodyType()) : null)
                .region(region)
                .nickName(progress.getNickName())
                .birthDate(progress.getBirthYear() != null ?
                        LocalDate.of(progress.getBirthYear(), 1, 1) : null)
                .mbti(progress.getMbti())
                .alcohol(progress.getDrinkingHabit() != null ?
                        DrinkingHabit.valueOf(progress.getDrinkingHabit()) : null)
                .smoking(progress.getSmokingHabit() != null ?
                        SmokingHabit.valueOf(progress.getSmokingHabit()) : null)
                .email(progress.getEmail())
                .university(university)
                .build();
    }

    /**
     * 사진 저장 시 첫 번째 이미지를 대표사진
     */
    private void saveProfileImages(Profile profile, Map<String, String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<Map.Entry<String, String>> sortedEntries = imageUrls.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> {
                    try {
                        return Integer.parseInt(e.getKey());
                    } catch (NumberFormatException ex) {
                        return Integer.MAX_VALUE; // 혹시 숫자 아니면 뒤로
                    }
                }))
                .toList();

        List<ProfileImage> images = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, String> entry : sortedEntries) {
            String storageUrl = entry.getValue(); // 원본 URL
            String fileName = extractFileNameFromStorageUrl(storageUrl);

            ProfileImage image = ProfileImage.builder()
                    .profile(profile)
                    .url(storageUrl)
                    .fileName(fileName)
                    .originalName("profile_" + entry.getKey())
                    .imageIndex(index)
                    .isMain(index == 0)
                    .build();

            images.add(image);
            index++;
        }

        profileImageRepository.saveAll(images);
    }

    /**
     * 질문 답변 저장
     */
    private void saveQuestionAnswers(Profile profile, Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        // 1. 필요한 모든 Question ID 수집
        Set<Long> questionIds = answers.keySet().stream()
                .map(this::mapQuestionKeyToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (questionIds.isEmpty()) {
            return;
        }

        // 2. 한 번에 모든 Question 조회
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        // 3. QuestionAnswer 엔터티들 생성
        List<QuestionAnswer> questionAnswers = new ArrayList<>();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String questionKey = entry.getKey();
            String answer = entry.getValue();

            if (answer == null || answer.trim().isEmpty()) {
                continue;
            }

            Long questionId = mapQuestionKeyToId(questionKey);
            Question question = questionMap.get(questionId);

            if (question != null) {
                QuestionAnswer questionAnswer = QuestionAnswer.builder()
                        .profile(profile)
                        .question(question)
                        .answer(answer)
                        .build();

                questionAnswers.add(questionAnswer);
            }
        }

        // 4. 배치로 한 번에 저장
        if (!questionAnswers.isEmpty()) {
            questionAnswerRepository.saveAll(questionAnswers);
            log.debug("질문 답변 저장 완료 - 개수: {}", questionAnswers.size());
        }
    }

    private Long mapQuestionKeyToId(String questionKey) {
        // Question 데이터 로드 시 설정된 ID와 매핑
        switch (questionKey) {
            // --- 필수 주관식 ---
            case "self_introduction": return 1L;
            case "attractive_partner_trait": return 2L;
            case "desired_partner_trait": return 3L;
            // --- 선택 주관식 ---
            case "meaningOfLove": return 4L;
            case "soulFood": return 5L;
            case "dailyAndHoliday": return 6L;
            case "idealDate": return 7L;
            // --- 필수 연애관 이지선다 ---
            case "relationship_conflictResolution": return 8L;
            case "relationship_photoSharing": return 9L;
            case "relationship_relationshipPriority": return 10L;
            case "relationship_datePlace": return 11L;
            case "relationship_jealousyAttitude": return 12L;
            case "relationship_idealDay": return 13L;
            case "relationship_attraction": return 14L;
            case "relationship_friendInteraction": return 15L;

            default:
                log.warn("알 수 없는 질문 키: {}", questionKey);
                return null;
        }
    }

    /**
     * 스토리지 URL에서 파일명 추출
     */
    private String extractFileNameFromStorageUrl(String storageUrl) {
        if (storageUrl == null) return null;
        int lastSlashIndex = storageUrl.lastIndexOf('/');
        return lastSlashIndex != -1 ? storageUrl.substring(lastSlashIndex + 1) : storageUrl;
    }

    /**
     * 토큰 쌍을 담는 내부 클래스
     */
    private static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
