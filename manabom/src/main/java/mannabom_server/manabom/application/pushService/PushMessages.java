package mannabom_server.manabom.application.pushService;

import mannabom_server.manabom.domain.pushMessage.PushMessage;

import java.util.Map;

public final class PushMessages {
    // 이 클래스는 생성자 사용하면 안됨
    private PushMessages(){}

    public static PushMessage likeReceived(Long fromUserId){
        return new PushMessage(
                "새 좋아요",
                "누군가 당신에게 호감을 보냈습니다!",
                Map.of(
                        "type", "LIKE_RECEIVED",
                        "fromUserId", String.valueOf(fromUserId)
                )
        );
    }

    public static PushMessage likeResponded(boolean accepted, Long toUserId) {
        return new PushMessage(
                "좋아요 결과",
                accepted ? "상대가 좋아요를 수락했습니다." : "상대가 좋아요를 거절했습니다.",
                Map.of(
                        "type", "LIKE_RESPONDED",
                        "accepted", String.valueOf(accepted),
                        "toUserId", String.valueOf(toUserId)
                )
        );
    }
}
