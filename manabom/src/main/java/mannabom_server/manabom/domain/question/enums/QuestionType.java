package mannabom_server.manabom.domain.question.enums;

import lombok.Getter;

@Getter
public enum QuestionType {
    REQUIRED_TEXT("필수 주관식"),      // 자기소개, 연인에게 바라는 점 등
    REQUIRED_CHOICE("필수 객관식"),    // 연애관 이지선다 질문들
    OPTIONAL_TEXT("선택 주관식");      // 나에게 연애란, 소울 푸드 등

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }
}
