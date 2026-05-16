package mannabom_server.manabom.application.signal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.signal.dto.enums.MatchType;
import mannabom_server.manabom.application.signal.dto.enums.Type;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SignalFromMeProfileDto {

    private Long id; // LIKE/MESSAGE: 해당 요청 객체 id, HIGH_SCORE: 상대 profileId
    private Type type; // LIKE | MESSAGE | HIGH_SCORE
    private MatchType matchType; // PROFILE | LOVE_VIEW, HIGH_SCORE는 null

    private String toUserNickname;
    private String toUserImageUrl; // LOVE_VIEW면 null
    private String message; // MESSAGE 외 null

    private String status; // PENDING | REJECTED, HIGH_SCORE는 null
    private String rejectReason; // 없으면 null

    private LocalDateTime receivedAt;
}
