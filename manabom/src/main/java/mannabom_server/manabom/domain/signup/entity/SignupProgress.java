package mannabom_server.manabom.domain.signup.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 회원가입 진행 상태 관리용 Redis 엔터티
 * TTL: 20분 (1200초)
 */
@RedisHash("signup_progress")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupProgress implements Serializable {

    @Id
    private String profileId;          // Redis Key로 사용

    private String kakaoId;            // 카카오 ID
    private String userName;           // 카카오에서 받은 이름 (실명)
    private Integer birthYear;         // 출생년도
    private String gender;             // 성별 (MALE/FEMALE)

    // 1단계: 기본 정보 + 연애관 질문 답변
    private Integer height;
    private String bodyType;           // BodyType enum의 String 값
    private String regionSido;
    private String regionSigungu;
    private String mbti;
    private String smokingHabit;       // SmokingHabit enum의 String 값
    private String drinkingHabit;      // DrinkingHabit enum의 String 값

    // 1단계에서 함께 받는 질문 답변들
    @Builder.Default
    private Map<String, String> questionAnswers = new HashMap<>();

    // 2단계: 닉네임 설정
    private String nickName;
    private Boolean nicknameVerified;

    // 3단계: 이메일 인증
    private String email;
    private String verificationCode;
    private Boolean emailVerified;

    // 4단계: 프로필 사진 업로드
    @Builder.Default
    private Map<String, String> profileImages = new HashMap<>();

    // 5단계: 약관 동의
    private Boolean serviceTermsAgreed;
    private Boolean privacyPolicyAgreed;
    private Boolean marketingConsentAgreed;

    // 진행 상태 관리
    private Integer currentStep;       // 현재 진행 중인 단계 (1~5)
    private Long completedAt;          // 완료 시간 (timestamp)

    @TimeToLive
    @Builder.Default
    private Long ttl = 1200L;          // 20분 TTL

    /**
     * 업데이트 메서드들
     * 1단계: 기본 정보 업데이트
     */
    public void updateBasicInfo(Integer height, String bodyType, String regionSido,
                                String regionSigungu, String mbti, String smokingHabit, String drinkingHabit) {
        this.height = height;
        this.bodyType = bodyType;
        this.regionSido = regionSido;
        this.regionSigungu = regionSigungu;
        this.mbti = mbti;
        this.smokingHabit = smokingHabit;
        this.drinkingHabit = drinkingHabit;
        updateCurrentStep(1);
    }

    /**
     * 1단계: 질문 답변 추가
     */
    public void addQuestionAnswer(String questionKey, String answer) {
        if (answer != null && !answer.trim().isEmpty()) {
            this.questionAnswers.put(questionKey, answer);
        }
    }

    /**
     * 2단계: 닉네임 설정
     */
    public void updateNickname(String nickName, Boolean verified) {
        this.nickName = nickName;
        this.nicknameVerified = verified;
        if (Boolean.TRUE.equals(verified)) {
            updateCurrentStep(2);
        }
    }

    /**
     * 3단계: 이메일 인증 정보 설정
     */
    public void updateEmail(String email, String verificationCode, Boolean verified) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.emailVerified = verified;
        if (Boolean.TRUE.equals(verified)) {
            updateCurrentStep(3);
        }
    }

    /**
     * 4단계: 프로필 이미지 추가
     */
    public void addProfileImage(String index, String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.profileImages.put(index, url);
            updateCurrentStep(4);
        }
    }

    /**
     * 5단계: 약관 동의
     */
    public void updateTermsAgreement(Boolean serviceTerms, Boolean privacyPolicy, Boolean marketingConsent) {
        this.serviceTermsAgreed = serviceTerms;
        this.privacyPolicyAgreed = privacyPolicy;
        this.marketingConsentAgreed = marketingConsent;
        if (Boolean.TRUE.equals(serviceTerms) && Boolean.TRUE.equals(privacyPolicy)) {
            updateCurrentStep(5);
        }
    }

    /**
     * 회원가입 완료 처리
     */
    public void markAsCompleted() {
        this.completedAt = System.currentTimeMillis();
        this.currentStep = 6; // 완료 상태
    }

    /**
     * 현재 단계 업데이트
     */
    private void updateCurrentStep(int step) {
        this.currentStep = Math.max(this.currentStep != null ? this.currentStep : 1, step);
    }


    /**
     * 검증 메서드들
     * 1단계 완료 여부 (기본 정보 + 필수 질문)
     */
    public boolean isStep1Completed() {
        boolean basicInfoComplete = height != null && bodyType != null && regionSido != null &&
                regionSigungu != null && mbti != null && smokingHabit != null && drinkingHabit != null;

        // 필수 질문 답변 확인 (3개: 자기소개, 매력적인 특징, 바라는 점)
        boolean requiredQuestionsComplete = questionAnswers.containsKey("self_introduction") &&
                questionAnswers.containsKey("attractive_partner_trait") &&
                questionAnswers.containsKey("desired_partner_trait");

        return basicInfoComplete && requiredQuestionsComplete;
    }

    /**
     * 2단계 완료 여부 (닉네임 검증)
     */
    public boolean isStep2Completed() {
        return nickName != null && !nickName.trim().isEmpty() && Boolean.TRUE.equals(nicknameVerified);
    }

    /**
     * 3단계 완료 여부 (이메일 인증)
     */
    public boolean isStep3Completed() {
        return email != null && !email.trim().isEmpty() && Boolean.TRUE.equals(emailVerified);
    }

    /**
     * 4단계 완료 여부 (프로필 사진)
     */
    public boolean isStep4Completed() {
        return profileImages != null && !profileImages.isEmpty();
    }

    /**
     * 5단계 완료 여부 (약관 동의)
     */
    public boolean isStep5Completed() {
        return Boolean.TRUE.equals(serviceTermsAgreed) && Boolean.TRUE.equals(privacyPolicyAgreed);
    }

    /**
     * 전체 단계 완료 여부
     */
    public boolean isAllStepsCompleted() {
        return isStep1Completed() && isStep2Completed() && isStep3Completed() &&
                isStep4Completed() && isStep5Completed();
    }

    /**
     * 특정 단계로 진행 가능한지 확인
     */
    public boolean canProceedToStep(int step) {
        switch (step) {
            case 1: return true;
            case 2: return isStep1Completed();
            case 3: return isStep2Completed();
            case 4: return isStep3Completed();
            case 5: return isStep4Completed();
            default: return false;
        }
    }

    /**
     * 다음 진행해야 할 단계 반환
     */
    public int getNextStep() {
        if (!isStep1Completed()) return 1;
        if (!isStep2Completed()) return 2;
        if (!isStep3Completed()) return 3;
        if (!isStep4Completed()) return 4;
        if (!isStep5Completed()) return 5;
        return 6; // 완료
    }
}