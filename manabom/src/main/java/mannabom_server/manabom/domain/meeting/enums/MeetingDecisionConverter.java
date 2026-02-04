package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MeetingDecisionConverter implements AttributeConverter<MeetingDecision,Byte> {
    @Override
    public Byte convertToDatabaseColumn(MeetingDecision attribute) {
        if(attribute==null) return null;
        return (byte)attribute.getCode();
    }

    @Override
    public MeetingDecision convertToEntityAttribute(Byte dbData) {
        if(dbData== null) return null;
        return MeetingDecision.getCode(dbData.intValue());
    }
}
