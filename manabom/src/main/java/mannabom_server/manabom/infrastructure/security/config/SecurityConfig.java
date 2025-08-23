package mannabom_server.manabom.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import mannabom_server.manabom.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Spring Security 설정 초기화 시작");

        http
                // CSRF 비활성화 (REST API + 모바일 앱)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용하지 않음 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 인증 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/api/auth/login/kakao",
                                "/api/auth/refresh",
                                "/api/signup/**",
                                "/api/questions/**",
                                "/api/signup/terms/**",
                                "/files/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // JWT 인증 실패 시 처리
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Spring Security 설정 초기화 완료");
        return http.build();
    }
}
