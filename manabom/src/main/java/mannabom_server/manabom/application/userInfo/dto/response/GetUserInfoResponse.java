package mannabom_server.manabom.application.userInfo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.application.userInfo.dto.common.ProfileDto;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;

import java.util.List;

@Getter
@AllArgsConstructor
public class GetUserInfoResponse {
    private final ProfileDto profile;
    private final List<QuestionAnswer> questionAnswerList;
}
