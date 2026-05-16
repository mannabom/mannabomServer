package mannabom_server.manabom.infrastructure.security.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminWebSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain adminWebFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin", "/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
