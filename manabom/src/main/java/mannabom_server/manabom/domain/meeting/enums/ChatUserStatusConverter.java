package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatUserStatusConverter implements AttributeConverter<ChatUserStatus,Short> {
    @Override
    public Short convertToDatabaseColumn(ChatUserStatus attribute) {
        return attribute==null? null : attribute.getCode();
    }

    @Override
    public ChatUserStatus convertToEntityAttribute(Short dbData) {
        return dbData==null? null: ChatUserStatus.from(dbData);
    }
}
