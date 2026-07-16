package mannabom_server.manabom.application.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatInitialSyncResponse {
    private boolean hasUnreadMessages;
    private boolean hasNewRoom;
}
