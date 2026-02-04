package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MeetingRoleConverter implements AttributeConverter<MeetingRole,Byte> {
    @Override
    public Byte convertToDatabaseColumn(MeetingRole attribute) {
        return attribute==null? null : (byte) attribute.getCode();
    }

    @Override
    public MeetingRole convertToEntityAttribute(Byte dbData) {
        return dbData==null? null : MeetingRole.from(dbData.intValue());
    }
}
