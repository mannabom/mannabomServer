package mannabom_server.manabom.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 이메일 서비스 설정
 */
@Configuration
public class EmailConfig {

    /**
     * 6자리 인증번호 생성기
     */
    @Bean
    public AuthCodeGenerator authCodeGenerator() {
        return () -> String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    @FunctionalInterface
    public interface AuthCodeGenerator {
        String generate();
    }
}