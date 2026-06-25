package mannabom_server.manabom.infrastructure.security.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.admin.repository.UserAccountRestrictionRepository;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class UserAccountAccessGuardFilter extends OncePerRequestFilter {

    private final UserAccountRestrictionRepository userAccountRestrictionRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            boolean blocked = userAccountRestrictionRepository.findById(userId)
                    .map(restriction -> restriction.isAccessBlocked(LocalDateTime.now()))
                    .orElse(false);
            if (blocked) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(response.getWriter(), Map.of(
                        "success", false,
                        "message", "관리자에 의해 제한된 계정입니다.",
                        "timestamp", LocalDateTime.now().toString()
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/admin/")
                || path.startsWith("/api/auth/login/")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/signup/")
                || path.startsWith("/api/questions/")
                || path.startsWith("/ws-chat/")
                || path.equals("/health")
                || path.equals("/error");
    }
}
