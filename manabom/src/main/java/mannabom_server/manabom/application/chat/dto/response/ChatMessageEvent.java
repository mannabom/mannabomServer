package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

//메세지 보내진 사실을 모든 참여자들에게 보내는 이벤트 서버->클
@Getter
@Builder
public class ChatMessageEvent {
    private Long roomId;

    private Long senderUserId;
    private String messageType;
    private String content;

    private Long messageId;
    private String clientMessageId;

    private Instant sendAt;

    private ChatGifticonInfo gifticon;
}
