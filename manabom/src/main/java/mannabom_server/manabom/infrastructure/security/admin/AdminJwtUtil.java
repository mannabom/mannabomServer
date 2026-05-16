package mannabom_server.manabom.infrastructure.security.admin;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;

@Component
@Getter
@Slf4j
public class AdminJwtUtil {

    private static final String TOKEN_TYPE_ADMIN = "ADMIN";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_LOGIN_ID = "login_id";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AdminJwtUtil(
            @Value("${app.admin.jwt.secret:mannabom-admin-super-secret-key-for-development-only-change-this}") String secret,
            @Value("${app.jwt.secret}") String userJwtSecret,
            @Value("${app.admin.jwt.access-token-expiration:3600000}") long accessTokenExpiration,
            @Value("${app.admin.jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration
    ) {
        if (secret.equals(userJwtSecret)) {
            throw new IllegalStateException("관리자 JWT secret은 사용자 JWT secret과 달라야 합니다.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(Long adminId, String loginId, Set<AdminRole> roles) {
        return generateToken(adminId, loginId, roles, accessTokenExpiration);
    }

    public String generateRefreshToken(Long adminId, String loginId, Set<AdminRole> roles) {
        return generateToken(adminId, loginId, roles, refreshTokenExpiration);
    }

    private String generateToken(Long adminId, String loginId, Set<AdminRole> roles, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);
        List<String> roleNames = roles.stream().map(AdminRole::name).toList();

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ADMIN)
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_LOGIN_ID, loginId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateAdminToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TOKEN_TYPE_ADMIN.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (Exception e) {
            log.debug("관리자 JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public AdminPrincipal getPrincipal(String token) {
        Claims claims = parseClaims(token);
        Long adminId = Long.parseLong(claims.getSubject());
        String loginId = claims.get(CLAIM_LOGIN_ID, String.class);
        List<?> roleNames = claims.get(CLAIM_ROLES, List.class);
        Set<AdminRole> roles = new LinkedHashSet<>();
        if (roleNames != null) {
            roleNames.forEach(role -> roles.add(AdminRole.valueOf(String.valueOf(role))));
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("관리자 권한 정보가 없는 토큰입니다.");
        }
        return new AdminPrincipal(adminId, loginId, roles);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
