package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.admin.entity.AdminAccount;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.admin.repository.AdminAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapService implements CommandLineRunner {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.login-id:${app.admin.bootstrap.login-id:}}")
    private String bootstrapLoginId;

    @Value("${app.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${app.admin.bootstrap.name:초기 관리자}")
    private String bootstrapName;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(bootstrapLoginId) || !StringUtils.hasText(bootstrapPassword)) {
            return;
        }
        if (adminAccountRepository.existsByLoginId(bootstrapLoginId)) {
            return;
        }

        adminAccountRepository.save(AdminAccount.builder()
                .loginId(bootstrapLoginId)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .name(bootstrapName)
                .roles(Set.of(AdminRole.SUPER_ADMIN))
                .build());
        log.warn("초기 SUPER_ADMIN 계정을 생성했습니다. loginId={}", bootstrapLoginId);
    }
}
