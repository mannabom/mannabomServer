package mannabom_server.manabom.application.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatInitialSyncResponse {
    private int unreadMessageCount;
    private boolean hasNewRoom;
}
