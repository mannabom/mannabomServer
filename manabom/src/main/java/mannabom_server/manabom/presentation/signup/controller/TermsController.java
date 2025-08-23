package mannabom_server.manabom.presentation.signup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.signup.dto.response.TermsContentResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 약관 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/signup/terms")
@RequiredArgsConstructor
@Slf4j
public class TermsController {

    /**
     * 약관 내용 조회
     */
    @GetMapping("/{termType}")
    public ResponseEntity<TermsContentResponseDto> getTermsContent(
            @PathVariable String termType) {

        log.info("약관 내용 조회 API 호출 - 유형: {}", termType);

        TermsContentResponseDto response = createTermsContent(termType);

        log.info("약관 내용 조회 API 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 약관 내용 생성 (실제 서비스에서는 DB에서 조회)
     */
    private TermsContentResponseDto createTermsContent(String termType) {
        String title;
        String content;
        boolean required;

        switch (termType) {
            case "service":
                title = "서비스 이용약관";
                content = buildServiceTermsContent();
                required = true;
                break;
            case "privacy":
                title = "개인정보 처리방침";
                content = buildPrivacyPolicyContent();
                required = true;
                break;
            case "marketing":
                title = "마케팅 정보 수신 동의";
                content = buildMarketingConsentContent();
                required = false;
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 약관 유형입니다.");
        }

        return TermsContentResponseDto.builder()
                .success(true)
                .data(TermsContentResponseDto.TermsContentDataDto.builder()
                        .termType(termType)
                        .title(title)
                        .content(content)
                        .lastUpdated("2024-01-01")
                        .required(required)
                        .build())
                .message("약관 내용 조회 성공")
                .build();
    }

    /**
     * 서비스 이용약관 내용
     */
    private String buildServiceTermsContent() {
        return """
                제1조 (목적)
                본 약관은 만나봄(이하 "회사")이 제공하는 대학생 소개팅 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.
                
                제2조 (정의)
                1. "서비스"란 회사가 제공하는 대학생 소개팅 매칭 서비스를 의미합니다.
                2. "이용자"란 본 약관에 따라 회사가 제공하는 서비스를 받는 회원을 의미합니다.
                
                제3조 (약관의 효력 및 변경)
                1. 본 약관은 서비스 화면에 게시하거나 기타의 방법으로 이용자에게 공지함으로써 효력이 발생합니다.
                2. 회사는 필요한 경우 본 약관을 변경할 수 있으며, 변경된 약관은 제1항과 같은 방법으로 공지합니다.
                
                제4조 (서비스의 제공 및 변경)
                1. 회사는 다음과 같은 서비스를 제공합니다:
                   - 대학생 프로필 매칭 서비스
                   - 채팅 서비스
                   - 미팅 주선 서비스
                
                제5조 (이용자의 의무)
                1. 이용자는 다음 행위를 하여서는 안 됩니다:
                   - 허위 정보 입력
                   - 타인의 개인정보 도용
                   - 서비스 운영 방해
                
                제6조 (개인정보보호)
                회사는 관련 법령이 정하는 바에 따라 이용자의 개인정보를 보호하기 위해 노력합니다.
                """;
    }

    /**
     * 개인정보 처리방침 내용
     */
    private String buildPrivacyPolicyContent() {
        return """
                제1조 (개인정보의 처리 목적)
                만나봄은 다음의 목적을 위하여 개인정보를 처리합니다:
                1. 회원가입 및 관리
                2. 서비스 제공
                3. 고객센터 운영
                
                제2조 (개인정보의 처리 및 보유기간)
                1. 개인정보 보유기간: 회원탈퇴 시까지
                2. 법령에 따른 보존의무가 있는 경우 해당 기간
                
                제3조 (처리하는 개인정보의 항목)
                1. 필수항목: 이름, 생년월일, 성별, 이메일, 대학교, 프로필 사진
                2. 선택항목: 연애관 정보, 취미 등
                
                제4조 (개인정보의 제3자 제공)
                회사는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다.
                
                제5조 (개인정보처리의 위탁)
                회사는 서비스 제공을 위해 필요한 경우 개인정보 처리를 위탁할 수 있습니다.
                
                제6조 (정보주체의 권리의무 및 행사방법)
                이용자는 개인정보 열람, 정정, 삭제, 처리정지 요구권을 가지고 있습니다.
                """;
    }

    /**
     * 마케팅 정보 수신 동의 내용
     */
    private String buildMarketingConsentContent() {
        return """
                제1조 (마케팅 정보 수신 동의)
                본 동의는 만나봄이 제공하는 마케팅 정보 수신에 대한 동의입니다.
                
                제2조 (수집하는 정보)
                1. 이메일 주소
                2. 서비스 이용 패턴
                
                제3조 (이용 목적)
                1. 신규 서비스 안내
                2. 이벤트 및 프로모션 정보 제공
                3. 맞춤형 광고 제공
                
                제4조 (정보 제공 방법)
                1. 이메일
                2. 앱 푸시 알림
                3. SMS (별도 동의 시)
                
                제5조 (동의철회)
                이용자는 언제든지 마케팅 정보 수신 동의를 철회할 수 있습니다.
                
                제6조 (동의 거부시 불이익)
                마케팅 정보 수신을 거부하더라도 서비스 이용에는 제한이 없습니다.
                """;
    }
}