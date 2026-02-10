package mannabom_server.manabom.application.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ProfileMatchConditionResponseDto {
    private Long profileId;
    private String nickName;
    private String profileImageUrl;
    private Integer age;
    private String mbti;
    private DrinkingHabit drinking;
    private SmokingHabit smoking;
}
