package mannabom_server.manabom.domain.user.enums;

import lombok.Getter;

@Getter
public enum BodyType {
    SLIM("마름"),
    AVERAGE("보통"),
    CHUBBY("통통");

    private final String description;

    BodyType(String description) {
        this.description = description;
    }
}
