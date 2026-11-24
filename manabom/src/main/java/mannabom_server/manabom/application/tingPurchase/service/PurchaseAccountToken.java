package mannabom_server.manabom.application.tingPurchase.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 스토어 영수증에 실어 보내는 구매자 식별 토큰.
 * 서버가 userId 로부터 결정적으로 만들고, 클라이언트는 결제 시 그대로 전달한다.
 * Apple appAccountToken 은 UUID 형식이어야 하므로 name-based UUID 를 사용한다.
 */
public final class PurchaseAccountToken {

    private static final String NAMESPACE = "mannabom:ting-purchase:user:";

    private PurchaseAccountToken() {
    }

    public static String forUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        return UUID.nameUUIDFromBytes((NAMESPACE + userId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static boolean matches(Long userId, String token) {
        return token != null && forUser(userId).equalsIgnoreCase(token.trim());
    }
}
