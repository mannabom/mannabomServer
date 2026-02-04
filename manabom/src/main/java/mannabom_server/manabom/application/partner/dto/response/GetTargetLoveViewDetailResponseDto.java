package mannabom_server.manabom.application.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.application.partner.dto.common.LikedDto;
import mannabom_server.manabom.application.partner.dto.common.MessagedDto;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTargetLoveViewDetailResponseDto {
    private String nickname;
    private int age;
    private String region;
    private List<QuestionAnswer> questionAnswers;
    private SmokingHabit smokingHabit;
    private DrinkingHabit drinkingHabit;
    private LikedDto liked;
    private MessagedDto messaged;
}
