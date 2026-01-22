package mannabom_server.manabom.application.pushService.service.pushSender;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.pushService.dto.response.PushBatchResult;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmPushSender implements PushSender {

    private final FirebaseApp firebaseApp;

    @Override
    public void sendToToken(String token, PushMessage msg) {
        log.info("firebase를 통해 전송 시작");
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);

        Message.Builder b = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(msg.title())
                        .setBody(msg.body())
                        .build());

        if (msg.data() != null) b.putAllData(msg.data());

        try {
            messaging.send(b.build());
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("FCM send failed", e);
        }
        log.info("firebase를 통해 전송 완료");
    }

    @Override
    public PushBatchResult sendToTokens(List<String> tokens, PushMessage msg) {
        if (tokens == null || tokens.isEmpty()) {
            return new PushBatchResult(0, 0, List.of());
        }

        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);

        MulticastMessage.Builder b = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(msg.title())
                        .setBody(msg.body())
                        .build());

        if (msg.data() != null) b.putAllData(msg.data());

        try {
            BatchResponse res = messaging.sendEachForMulticast(b.build());
            List<String> invalid = new ArrayList<>();
            for (int i = 0; i < res.getResponses().size(); i++) {
                SendResponse r = res.getResponses().get(i);
                if (!r.isSuccessful()) {
                    FirebaseMessagingException ex = (FirebaseMessagingException) r.getException();
                    MessagingErrorCode code = ex.getMessagingErrorCode();

                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalid.add(tokens.get(i));
                    }
                    log.info("push 전송 실패 : {}\n에러 코드 : {}", tokens.get(i),code);
                }else{
                    log.info("push 전송 성공 : {}", tokens.get(i));
                }
            }
            return new PushBatchResult(res.getSuccessCount(), res.getFailureCount(), invalid);

        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("FCM multicast failed", e);
        }
    }
}

