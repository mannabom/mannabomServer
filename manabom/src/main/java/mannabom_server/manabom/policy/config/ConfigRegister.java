package mannabom_server.manabom.policy.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 각 객체에 application.yml의 값을 찾아서 넣어줌
 */
@Configuration
@EnableConfigurationProperties({
        MatchPolicyProperties.class,
        BenefitPolicyProperties.class,
        TingPolicyProperties.class
})
public class ConfigRegister {
}
