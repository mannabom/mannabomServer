package mannabom_server.manabom.domain.chat.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatRoomTypeConverter implements AttributeConverter<ChatRoomType,Short> {
    @Override
    public Short convertToDatabaseColumn(ChatRoomType chatRoomType) {
        return chatRoomType==null ? null : chatRoomType.getCode();
    }

    @Override
    public ChatRoomType convertToEntityAttribute(Short aShort) {
        return aShort==null ? null : ChatRoomType.from(aShort);
    }
}
