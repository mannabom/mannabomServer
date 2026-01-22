package mannabom_server.manabom.domain.chat.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatMessageTypeConverter implements AttributeConverter<ChatMessageType, Short> {
    @Override
    public Short convertToDatabaseColumn(ChatMessageType chatMessageType) {
        return chatMessageType==null ? null : chatMessageType.getCode();
    }

    @Override
    public ChatMessageType convertToEntityAttribute(Short aShort) {
        return aShort==null ? null : ChatMessageType.from(aShort);
    }
}
