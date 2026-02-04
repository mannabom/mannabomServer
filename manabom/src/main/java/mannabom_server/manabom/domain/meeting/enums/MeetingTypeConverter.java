package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MeetingTypeConverter implements AttributeConverter<MeetingType,Byte> {
    @Override
    public Byte convertToDatabaseColumn(MeetingType meetingType) {
        return meetingType==null? null : (byte)meetingType.getCode();
    }

    @Override
    public MeetingType convertToEntityAttribute(Byte aByte) {
        return aByte==null? null : MeetingType.from(aByte.intValue());
    }
}
