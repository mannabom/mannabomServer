package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatUserStatusConverter implements AttributeConverter<ChatUserStatus,Byte> {
    @Override
    public Byte convertToDatabaseColumn(ChatUserStatus attribute) {
        return attribute==null? null : (byte) attribute.getCode();
    }

    @Override
    public ChatUserStatus convertToEntityAttribute(Byte dbData) {
        return dbData==null? null: ChatUserStatus.from(dbData.intValue());
    }
}
