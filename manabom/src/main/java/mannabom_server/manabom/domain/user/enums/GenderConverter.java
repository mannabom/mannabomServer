package mannabom_server.manabom.domain.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender,Short> {


    @Override
    public Short convertToDatabaseColumn(Gender gender) {
        return (gender==null)? null : (short) gender.getCode();
    }

    @Override
    public Gender convertToEntityAttribute(Short aShort) {
        return (aShort == null)? null : Gender.from(aShort);
    }
}
