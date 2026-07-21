package mannabom_server.manabom.application.chat.dto.event;

public record ChatSystemMessageEvent(
        Long roomId,
        String content
) {
}
