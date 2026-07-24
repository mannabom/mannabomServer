package mannabom_server.manabom.application.gifticon.port.command;

public record GifticonOrderCommand(
        String templateToken,
        String receiverPhone,
        String receiverName,
        String externalKey,
        String externalOrderId
) {
}
