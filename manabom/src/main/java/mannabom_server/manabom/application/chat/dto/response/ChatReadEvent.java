package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChatReadEvent {
    private Long roomId;
    private Long userId;
    private Long lastReadMessageId;
}
