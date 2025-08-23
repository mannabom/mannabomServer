package mannabom_server.manabom.application.signup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.signup.repository.EmailVerificationRepository;
import mannabom_server.manabom.infrastructure.config.EmailConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스 (실제 발송 + 더미 모드)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailConfig.AuthCodeGenerator authCodeGenerator;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.dummy-mode:false}")
    private boolean dummyMode;

    /**
     * 대학 이메일 인증번호 발송
     */
    public String sendVerificationCode(String email) {
        log.info("이메일 인증번호 발송 요청 - 이메일: {}", email);

        try {
            // 1. 인증번호 생성
            String verificationCode = authCodeGenerator.generate();

            // 2. Redis에 저장
            emailVerificationRepository.saveVerificationCode(email, verificationCode);

            // 3. 이메일 발송
            if (dummyMode) {
                // 개발환경: 콘솔 출력만
                log.warn("개발용 인증번호 [{}]: {}", email, verificationCode);
                log.warn("dummy-mode=true이므로 실제 이메일은 발송되지 않습니다.");
            } else {
                // 운영환경: 실제 이메일 발송
                sendEmail(email, verificationCode);
            }

            log.info("이메일 인증번호 발송 완료 - 이메일: {}", email);
            return verificationCode;

        } catch (IllegalArgumentException e) {
            // 재발송 제한 예외는 그대로 던지기
            log.warn("이메일 인증번호 재발송 제한 - 이메일: {}, 메시지: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증번호 발송 실패 - 이메일: {}", email, e);
            throw new RuntimeException("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * 실제 이메일 발송
     */
    private void sendEmail(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[만나봄] 대학 인증번호");
            message.setText(createEmailContent(verificationCode));

            mailSender.send(message);
            log.info("이메일 발송 완료 - 수신자: {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 발송 실패 - 수신자: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 내용 생성
     */
    private String createEmailContent(String verificationCode) {
        return String.format(
                """
                안녕하세요! 만나봄입니다.
                
                대학생 인증을 위한 인증번호를 보내드립니다.
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                인증번호: %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                위 인증번호를 3분 이내에 앱에서 입력해 주세요.
                
                ※ 본인이 요청하지 않은 인증번호라면 이 메일을 무시해 주세요.
                ※ 인증번호 재발송은 1분 간격으로 가능합니다.
                
                감사합니다.
                만나봄 팀 드림
                """,
                verificationCode
        );
    }

    /**
     * 인증번호 확인
     */
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = emailVerificationRepository.getVerificationCode(email);
        boolean isValid = inputCode != null && inputCode.equals(storedCode);

        if (isValid) {
            log.info("이메일 인증 성공 - 이메일: {}", email);
            emailVerificationRepository.deleteVerificationCode(email);
        } else {
            log.warn("이메일 인증 실패 - 이메일: {}, 입력코드: {}", email, inputCode);
        }

        return isValid;
    }

    /**
     * 인증번호 삭제 (인증 완료 시)
     */
    public void clearVerificationCode(String email) {
        emailVerificationRepository.deleteVerificationCode(email);
        log.debug("인증번호 삭제 완료 - 이메일: {}", email);
    }

    /**
     * 재발송 가능 여부 확인
     */
    public boolean canResendVerificationCode(String email) {
        return emailVerificationRepository.canResendVerificationCode(email);
    }

    /**
     * 재발송 제한 남은 시간 확인 (UI 에서 타이머 표시할 거라면)
     */
    public long getRemainingCooldownSeconds(String email) {
        long ttl = emailVerificationRepository.getRateLimitTTL(email);
        return ttl > 0 ? ttl : 0;
    }
}