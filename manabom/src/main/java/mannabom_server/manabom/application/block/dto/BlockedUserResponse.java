package mannabom_server.manabom.application.block.dto;

import java.time.Instant;

/**
 * 차단 목록 항목. 프론트는 profileId 로 회원을 식별한다 (Long 은 JacksonConfig 에서 문자열로 직렬화).
 */
public record BlockedUserResponse(
        Long profileId,
        String nickName,
        Instant blockedAt
) {
}
