package mannabom_server.manabom.application.chat.dto.response;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageResponseTest {

    @Test
    void convertsSystemMessageWithoutSender() {
        ChatMessage message = ChatMessage.system(
                ChatRoom.builder().id(77L).build(),
                "시스템 알림 본문",
                "PHOTO_REQUESTED",
                "봄이님이 프로필 공개를 요청했어요",
                1L,
                "봄이",
                Map.of("status", "RECEIVED")
        );

        ChatMessageResponse response = ChatMessageResponse.of(message);

        assertThat(response.getMessageType()).isEqualTo("SYSTEM");
        assertThat(response.getSenderId()).isNull();
        assertThat(response.getContent()).isEqualTo("시스템 알림 본문");
        assertThat(response.getSystemTitle()).isEqualTo("봄이님이 프로필 공개를 요청했어요");
        assertThat(response.getActorNickname()).isEqualTo("봄이");
        assertThat(response.getData()).containsEntry("status", "RECEIVED");
    }
}
