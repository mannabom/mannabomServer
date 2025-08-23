package mannabom_server.manabom.presentation.signup.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.signup.dto.request.*;
import mannabom_server.manabom.application.signup.dto.response.*;
import mannabom_server.manabom.application.signup.service.SignupService;
import org.hibernate.mapping.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 회원가입 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/signup")
@RequiredArgsConstructor
@Slf4j
public class SignupController {

    private final SignupService signupService;

    /**
     * 1단계: 기본 프로필 정보 및 연애관 저장
     */
    @PostMapping("/profile-relationship")
    public ResponseEntity<ProfileRelationshipResponseDto> saveProfileRelationship(
            @Valid @RequestBody ProfileRelationshipRequestDto request) {

        log.info("기본 프로필 정보 저장 API 호출 - Profile ID: {}", request.getProfileId());

        ProfileRelationshipResponseDto response = signupService.saveProfileRelationship(request);

        log.info("기본 프로필 정보 저장 API 완료 - Profile ID: {}",
                response.getData().getProfileId());

        return ResponseEntity.ok(response);
    }

    /**
     * 2단계: 닉네임 중복 확인
     */
    @PostMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponseDto> checkNickname(
            @Valid @RequestBody NicknameCheckRequestDto request) {

        log.info("닉네임 중복 확인 API 호출 - 닉네임: {}", request.getNickname());

        NicknameCheckResponseDto response = signupService.checkNickname(request);

        log.info("닉네임 중복 확인 API 완료 - 사용가능: {}",
                response.getData().isAvailable());

        return ResponseEntity.ok(response);
    }

    /**
     * 2단계: 닉네임 설정
     */
    @PostMapping("/set-nickname")
    public ResponseEntity<SetNicknameResponseDto> setNickname(
            @Valid @RequestBody SetNicknameRequestDto request) {

        log.info("닉네임 설정 API 호출 - Profile ID: {}, 닉네임: {}",
                request.getProfileId(), request.getNickname());

        SetNicknameResponseDto response = signupService.setNickname(request);

        log.info("닉네임 설정 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 3단계: 대학 이메일 인증번호 발송
     */
    @PostMapping("/send-email-verification")
    public ResponseEntity<SendEmailVerificationResponseDto> sendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequestDto request) {

        log.info("이메일 인증번호 발송 API 호출 - Profile ID: {}, 이메일: {}",
                request.getProfileId(), request.getEmail());

        SendEmailVerificationResponseDto response = signupService.sendEmailVerification(request);

        log.info("이메일 인증번호 발송 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 3단계: 이메일 인증번호 확인
     */
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponseDto> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDto request) {

        log.info("이메일 인증번호 확인 API 호출 - Profile ID: {}", request.getProfileId());

        VerifyEmailResponseDto response = signupService.verifyEmail(request);

        log.info("이메일 인증번호 확인 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 4단계: 프로필 사진 업로드
     */
    @PostMapping(value = "/profile-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePhotosResponseDto> uploadProfilePhotos(
            @RequestParam("profileId") String profileId,
            @RequestParam("photos") List<MultipartFile> photos) {

        log.info("프로필 사진 업로드 API 호출 - Profile ID: {}, 사진 개수: {}",
                profileId, photos.size());

        ProfilePhotosRequestDto request = new ProfilePhotosRequestDto(profileId, photos);
        ProfilePhotosResponseDto response = signupService.uploadProfilePhotos(request);

        log.info("프로필 사진 업로드 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 5단계: 약관 동의
     */
    @PostMapping("/terms-agreement")
    public ResponseEntity<TermsAgreementResponseDto> agreeToTerms(
            @Valid @RequestBody TermsAgreementRequestDto request) {

        log.info("약관 동의 API 호출 - Profile ID: {}", request.getProfileId());

        TermsAgreementResponseDto response = signupService.agreeToTerms(request);

        log.info("약관 동의 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 6단계: 회원가입 완료
     */
    @PostMapping("/complete")
    public ResponseEntity<SignupCompleteResponseDto> completeSignup(
            @Valid @RequestBody SignupCompleteRequestDto request) {

        log.info("회원가입 완료 API 호출 - Profile ID: {}", request.getProfileId());

        SignupCompleteResponseDto response = signupService.completeSignup(request);

        log.info("회원가입 완료 API 완료 - 사용자 ID: {}",
                response.getData().getUserId());

        return ResponseEntity.ok(response);
    }
}