package mannabom_server.manabom.application.userInfo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.application.userInfo.dto.common.ProfileDto;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;

import java.util.List;

@AllArgsConstructor
@Getter
public class PutUserInfoRequest {
    private final ProfileDto profile;
    private final List<QuestionAnswer> answers;
}
