package mannabom_server.manabom.domain.meeting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role,Short> {
    @Override
    public Short convertToDatabaseColumn(Role attribute) {
        return attribute==null? null : attribute.getCode();
    }

    @Override
    public Role convertToEntityAttribute(Short dbData) {
        return dbData==null? null : Role.from(dbData);
    }
}
