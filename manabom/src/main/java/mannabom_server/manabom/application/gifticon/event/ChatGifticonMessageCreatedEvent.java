package mannabom_server.manabom.application.gifticon.event;

import mannabom_server.manabom.application.chat.dto.response.ChatGifticonInfo;

import java.time.Instant;

public record ChatGifticonMessageCreatedEvent(
        Long roomId,
        Long messageId,
        Long senderUserId,
        Long receiverUserId,
        String senderNickname,
        String content,
        Instant createdAt,
        ChatGifticonInfo gifticon
) {
}
