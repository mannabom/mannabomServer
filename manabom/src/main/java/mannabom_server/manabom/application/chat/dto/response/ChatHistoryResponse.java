package mannabom_server.manabom.application.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ChatHistoryResponse {
    private Long roomId;
    private List<ChatMessageResponse> messages;
    private boolean hasNext;
}
