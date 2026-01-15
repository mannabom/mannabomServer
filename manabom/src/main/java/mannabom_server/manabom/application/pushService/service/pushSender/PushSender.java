package mannabom_server.manabom.application.pushService.service.pushSender;

import mannabom_server.manabom.application.pushService.dto.response.PushBatchResult;
import mannabom_server.manabom.domain.pushMessage.PushMessage;

import java.util.List;

public interface PushSender {
    void sendToToken(String token, PushMessage msg);
    PushBatchResult sendToTokens(List<String> tokens, PushMessage msg);
}

