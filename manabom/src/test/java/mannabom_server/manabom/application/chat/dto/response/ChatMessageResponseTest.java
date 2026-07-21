package mannabom_server.manabom.application.chat.dto.response;

import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageResponseTest {

    @Test
    void convertsSystemMessageWithoutSender() {
        ChatMessage message = ChatMessage.system(
                ChatRoom.builder().id(77L).build(),
                "시스템 알림"
        );

        ChatMessageResponse response = ChatMessageResponse.of(message);

        assertThat(response.getMessageType()).isEqualTo("SYSTEM");
        assertThat(response.getSenderId()).isNull();
        assertThat(response.getContent()).isEqualTo("시스템 알림");
    }
}
