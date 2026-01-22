package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MeetingTypeConverter implements AttributeConverter<MeetingType,Short> {
    @Override
    public Short convertToDatabaseColumn(MeetingType meetingType) {
        return meetingType==null? null : meetingType.getCode();
    }

    @Override
    public MeetingType convertToEntityAttribute(Short aShort) {
        return aShort==null? null : MeetingType.from(aShort);
    }
}
