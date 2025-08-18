package mannabom_server.manabom.domain.user.enums;

import lombok.Getter;

@Getter
public enum SmokingHabit {
    NON_SMOKER("비흡연"),
    VAPE_ONLY("전자담배"),
    REGULAR_SMOKER("흡연");

    private final String description;

    SmokingHabit(String description) {
        this.description = description;
    }
}
