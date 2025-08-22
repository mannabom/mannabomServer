package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.question.enums.QuestionType;

import java.util.List;

@Getter
@Builder
public class QuestionListResponseDto {
    private boolean success;
    private QuestionListDataDto data;
    private String message;

    @Getter
    @Builder
    public static class QuestionListDataDto {
        private List<QuestionDto> questions;
    }

    @Getter
    @Builder
    public static class QuestionDto {
        private Long questionId;
        private String question;
        private QuestionType questionType;
    }
}