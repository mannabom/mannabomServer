package mannabom_server.manabom.application.signal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import mannabom_server.manabom.application.signal.dto.enums.MatchType;
import mannabom_server.manabom.application.signal.dto.enums.Type;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignalToMeProfileDto {
    private Long targetProfileId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long requestId; // LIKE/MESSAGE 요청 객체 id, HIGH_SCORE에는 미포함
    private MatchType matchType; // PROFILE | LOVE_VIEW
    private Type type; // LIKE | MESSAGE | HIGH_SCORE
    private String fromUserNickname;
    private String fromUserImageUrl; // loveView 매칭이면 null
    private String message; // LIKE/HIGH_SCORE면 null, MESSAGE에서만 사용
    private LocalDateTime receivedAt;
}
