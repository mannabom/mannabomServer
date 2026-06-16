package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ChatSyncResponse {
    private Long roomId;
    private List<ChatMemberInfo> members;
    private List<ChatMessageResponse> messages;
    private boolean hasGap;
}
