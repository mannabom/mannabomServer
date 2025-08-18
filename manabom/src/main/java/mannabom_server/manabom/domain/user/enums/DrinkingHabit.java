package mannabom_server.manabom.domain.user.enums;

import lombok.Getter;

@Getter
public enum DrinkingHabit {
    NON_DRINKER("안 마심"),
    OCCASIONAL_DRINKER("가끔 음주"),
    FREQUENT_DRINKER("자주 음주");

    private final String description;

    DrinkingHabit(String description) {
        this.description = description;
    }
}
