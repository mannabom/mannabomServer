package mannabom_server.manabom.domain.chat.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatStatusConverter implements AttributeConverter<ChatStatus,Short> {
    @Override
    public Short convertToDatabaseColumn(ChatStatus chatStatus) {
        return chatStatus==null? null: chatStatus.getCode();
    }

    @Override
    public ChatStatus convertToEntityAttribute(Short aShort) {
        return aShort==null? null: ChatStatus.from(aShort);
    }
}
