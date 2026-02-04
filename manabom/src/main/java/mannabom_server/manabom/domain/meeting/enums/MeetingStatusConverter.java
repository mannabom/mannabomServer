package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MeetingStatusConverter implements AttributeConverter<MeetingStatus,Byte> {
    @Override
    public Byte convertToDatabaseColumn(MeetingStatus meetingStatus) {
        return meetingStatus ==null? null : (byte) meetingStatus.getCode();
    }

    @Override
    public MeetingStatus convertToEntityAttribute(Byte aByte) {
        return aByte==null? null : MeetingStatus.from(aByte.intValue());
    }
}
