package mannabom_server.manabom.application.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 갱신 응답 DTO
 */
@Getter
@Builder
public class RefreshTokenResponseDto {

    private boolean success;                // 요청 성공 여부
    private RefreshTokenDataDto data;       // 토큰 데이터
    private String message;                 // 결과 메시지

    /**
     * 토큰 갱신 성공 응답 생성
     */
    public static RefreshTokenResponseDto of(String accessToken, String refreshToken, long expiresIn) {
        RefreshTokenDataDto data = RefreshTokenDataDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();

        return RefreshTokenResponseDto.builder()
                .success(true)
                .data(data)
                .message("토큰 갱신 성공")
                .build();
    }

    /**
     * 토큰 데이터 DTO
     */
    @Getter
    @Builder
    public static class RefreshTokenDataDto {
        private String accessToken;        // 새로운 액세스 토큰
        private String refreshToken;       // 새로운 리프레시 토큰
        private long expiresIn;            // 만료 시간 (초 단위)
    }
}
