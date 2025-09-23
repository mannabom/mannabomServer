package mannabom_server.manabom.application.userInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.entity.Profile;

import java.util.List;

@AllArgsConstructor
@Getter
public class PutUserInfoRequest {
    private final Profile profile;
    private final List<QuestionAnswer> answers;
}
