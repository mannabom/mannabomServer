package mannabom_server.manabom.application.matching.loveViewMatching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoveViewMatchConditionResponseDto {
    private final Long profileId;
    private final int age;
    private final String mbti;
    private final DrinkingHabit drinkingHabit;
    private final SmokingHabit smokingHabit;
    private final List<QuestionAnswer> questionAnswers;
}
