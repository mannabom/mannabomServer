package mannabom_server.manabom.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 정보 추출 담당하는 유틸리티 클래스
 */
@Component
@Slf4j
@Getter
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("JWT 유틸리티 초기화 완료 - Access Token: {}ms, Refresh Token: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    /**
     * JWT 토큰 생성 (공통 메서드)
     */
    private String generateToken(Long userId, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))        // userId를 subject로 설정
                .issuedAt(Date.from(now))               // 발급 시간
                .expiration(Date.from(expiration))      // 만료 시간
                .signWith(secretKey)
                .compact();
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpiration);
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshTokenExpiration);
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)          // 비밀키로 서명 검증
                    .build()
                    .parseSignedClaims(token)       // 토큰을 파싱하여 Claims 객체 생성
                    .getPayload();                  // Payload(실제 데이터) 부분 추출

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.", e);
        }
    }

    /**
     * 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (SecurityException ex) {        // JWT 서명이 잘못된 경우
            log.debug("잘못된 JWT 서명: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {    // JWT 형식이 잘못된 경우
            log.debug("잘못된 JWT 토큰: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {      // JWT 토큰이 만료된 경우
            log.debug("만료된 JWT 토큰: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {  // 지원하지 않는 JWT 형식인 경우
            log.debug("지원되지 않는 JWT 토큰: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) { // JWT 클레임이 비어있거나 잘못된 경우
            log.debug("JWT 클레임이 비어있음: {}", ex.getMessage());
        } catch (Exception ex) {
            log.debug("JWT 토큰 검증 실패: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 토큰 만료 시간 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}