package mannabom_server.manabom.application.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class GetTargetProfileDetailResponseDto {
    private String nickname;
    private int age;
    private String region;
    private List<QuestionAnswer> questionAnswers;
    private List<Photo> photos;
    private SmokingHabit smoking;
    private DrinkingHabit drinking;
    private Liked liked;
    private Messaged messaged;

    @Getter
    @AllArgsConstructor
    public static class Photo {
        private Long photoId;
        private String imageUrl;
        private boolean blind;
    }

    @Getter
    @AllArgsConstructor
    public static class Liked {
        boolean sent;
        LikeStatus likeStatus;
    }

    @Getter
    @AllArgsConstructor
    public static class Messaged {
        boolean sent;
        MessageRequestStatus messageStatus;
    }
}
