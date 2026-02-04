package mannabom_server.manabom.application.matching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.Setter;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

@Getter
@AllArgsConstructor
public class MatchConditionRequestDto {
    @NotNull(message = "최소 나이가 비어있습니다.")
    private final Integer minAge;
    @NotNull(message = "최대 나이가 비어있습니다.")
    private final Integer maxAge;
    @NotNull(message = "음주여부가 선택되지 않았습니다.")
    private final List<DrinkingHabit> drinking;
    @NotNull(message = "흡연여부가 선택되지 않았습니다.")
    private final List<SmokingHabit> smoking;
}
