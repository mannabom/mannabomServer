package mannabom_server.manabom.application.meeting.dto.raw;

import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.time.LocalDate;

public record TeamMemberDetailRaw(
        Long userId,
        String nickname,
        LocalDate birthDate,
        String mbti,
        SmokingHabit smokingHabit,
        DrinkingHabit drinkingHabit,
        String mainImageUrl
) {
}
