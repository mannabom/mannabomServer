package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MatchingStatusConverter implements AttributeConverter<MatchingStatus,Short> {
    @Override
    public Short convertToDatabaseColumn(MatchingStatus matchingStatus) {
        return matchingStatus==null? null : matchingStatus.getCode();
    }

    @Override
    public MatchingStatus convertToEntityAttribute(Short aShort) {
        return aShort==null? null : MatchingStatus.from(aShort);
    }
}
