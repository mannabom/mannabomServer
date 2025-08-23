package mannabom_server.manabom.domain.signup.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 이메일 인증번호 관리용 Redis 레포지토리 - 직접 구현
 */
@Repository
@Slf4j
public class EmailVerificationRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public EmailVerificationRepository(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";
    private static final String EMAIL_RATE_LIMIT_PREFIX = "email_rate_limit:";
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(3); // 3분
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(30);       // 30초 재발송 제한

    /**
     * 인증번호 저장
     */
    public void saveVerificationCode(String email, String code) {
        try {
            String normalizedEmail = email.toLowerCase().trim();

            String rateLimitKey = EMAIL_RATE_LIMIT_PREFIX + normalizedEmail;
            if (redisTemplate.hasKey(rateLimitKey)) {
                Long remainingTTL = redisTemplate.getExpire(rateLimitKey);
                throw new IllegalArgumentException(
                        String.format("인증번호 재발송은 %d초 후 가능합니다.", remainingTTL)
                );
            }

            String verificationKey = EMAIL_VERIFICATION_PREFIX + normalizedEmail;
            redisTemplate.opsForValue().set(verificationKey, code, VERIFICATION_CODE_TTL);

            redisTemplate.opsForValue().set(rateLimitKey, "sent", RATE_LIMIT_TTL);

            log.debug("이메일 인증번호 저장 완료 - 이메일: {}, 재발송 제한 설정", normalizedEmail);

        } catch (IllegalArgumentException e) {
            // 재발송 제한 예외는 그대로 던지기
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증번호 저장 실패 - 이메일: {}", email, e);
            throw new RuntimeException("인증번호 저장에 실패했습니다.", e);
        }
    }

    /**
     * 인증번호 조회
     */
    public String getVerificationCode(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String key = EMAIL_VERIFICATION_PREFIX + normalizedEmail;
            String code = redisTemplate.opsForValue().get(key);
            log.debug("이메일 인증번호 조회 - 이메일: {}, 존재여부: {}", normalizedEmail, code != null);
            return code;
        } catch (Exception e) {
            log.error("이메일 인증번호 조회 실패 - 이메일: {}", email, e);
            return null;
        }
    }

    /**
     * 인증번호 삭제 (인증 완료 시)
     */
    public void deleteVerificationCode(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String verificationKey = EMAIL_VERIFICATION_PREFIX + normalizedEmail;
            String rateLimitKey = EMAIL_RATE_LIMIT_PREFIX + normalizedEmail;

            Boolean verificationDeleted = redisTemplate.delete(verificationKey);
            Boolean rateLimitDeleted = redisTemplate.delete(rateLimitKey);

            log.debug("이메일 인증번호 삭제 완료 - 이메일: {}, 인증번호삭제: {}, 제한삭제: {}",
                    normalizedEmail, verificationDeleted, rateLimitDeleted);
        } catch (Exception e) {
            log.error("이메일 인증번호 삭제 실패 - 이메일: {}", email, e);
        }
    }

    /**
     * 인증번호 존재 여부 확인
     */
    public boolean existsVerificationCode(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String key = EMAIL_VERIFICATION_PREFIX + normalizedEmail;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("이메일 인증번호 존재 여부 확인 실패 - 이메일: {}", email, e);
            return false;
        }
    }

    /**
     * 인증번호 남은 만료 시간 조회 - 굳이 필요는 없는데, 시간 흘러가는거 보이게 하려면 활용.
     */
    public long getVerificationCodeTTL(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String key = EMAIL_VERIFICATION_PREFIX + normalizedEmail;
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("인증번호 TTL 조회 실패 - 이메일: {}", email, e);
            return -1;
        }
    }

    /**
     * 재발송 제한 상태 확인 (남은 시간 반환)
     */
    public long getRateLimitTTL(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String key = EMAIL_RATE_LIMIT_PREFIX + normalizedEmail;
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("재발송 제한 TTL 조회 실패 - 이메일: {}", email, e);
            return -1;
        }
    }

    /**
     * 재발송 가능 여부 확인
     */
    public boolean canResendVerificationCode(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String key = EMAIL_RATE_LIMIT_PREFIX + normalizedEmail;
            return !redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("재발송 가능 여부 확인 실패 - 이메일: {}", email, e);
            return false;
        }
    }
}