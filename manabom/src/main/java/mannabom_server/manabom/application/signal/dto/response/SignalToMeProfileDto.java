package mannabom_server.manabom.application.signal.dto.response;

import lombok.*;
import mannabom_server.manabom.application.signal.dto.enums.MatchType;
import mannabom_server.manabom.application.signal.dto.enums.Type;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignalToMeProfileDto {
    private Long id;
    private MatchType matchType; // PROFILE | LOVE_VIEW
    private Type type; // LIKE | MESSAGE | HIGH_SCORE
    private String fromUserNickname;
    private String fromUserImageUrl; // loveView 매칭이면 null
    private String message; // LIKE/HIGH_SCORE면 null, MESSAGE에서만 사용
    private LocalDateTime receivedAt;
}
