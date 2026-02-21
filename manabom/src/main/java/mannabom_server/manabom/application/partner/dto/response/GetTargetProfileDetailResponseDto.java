package mannabom_server.manabom.application.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.partner.dto.common.LikedDto;
import mannabom_server.manabom.application.partner.dto.common.MessagedDto;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.enums.BodyType;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class GetTargetProfileDetailResponseDto {
    private String nickname;
    private int age;
    private int height;
    private BodyType bodyType;
    private String region;
    private List<QuestionAnswer> questionAnswers;
    private List<Photo> photos;
    private SmokingHabit smoking;
    private DrinkingHabit drinking;
    private LikedDto liked;
    private MessagedDto messaged;

    @Getter
    @AllArgsConstructor
    public static class Photo {
        private Long photoId;
        private String imageUrl;
        private boolean blind;
    }
}
