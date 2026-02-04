package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter
public class MatchingStatusConverter implements AttributeConverter<MatchingStatus,Byte> {
    @Override
    public Byte convertToDatabaseColumn(MatchingStatus attribute) {
        if(attribute == null) return null;
        return (byte)attribute.getCode();
    }

    @Override
    public MatchingStatus convertToEntityAttribute(Byte dbData) {
        if(dbData == null) return null;
        return MatchingStatus.fromCode(dbData.intValue());
    }
}
