package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

//메세지 보내진 사실을 모든 참여자들에게 보내는 이벤트 서버->클
@Getter
@Builder
public class ChatMessageEvent {
    private Long roomId;

    private Long senderUserId;
    private Long actorUserId;
    private List<Long> recipientUserIds;
    private String messageType;
    private String systemEventType;
    private String systemTitle;
    private String actorNickname;
    private String content;
    private Map<String, Object> data;

    private Long messageId;
    private String clientMessageId;

    private Instant sendAt;
}
