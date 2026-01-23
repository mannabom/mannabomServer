package mannabom_server.manabom.application.matching.profileMatching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

@Getter
@AllArgsConstructor
public class ProfileMatchConditionRequestDto {
    @NotNull(message = "최소 나이가 비어있습니다.")
    final private Integer minAge;
    @NotNull(message = "최대 나이가 비어있습니다.")
    final private Integer maxAge;
    @NotNull(message = "음주여부가 선택되지 않았습니다.")
    List<DrinkingHabit> drinking;
    @NotNull(message = "흡연여부가 선택되지 않았습니다.")
    List<SmokingHabit> smoking;
}
