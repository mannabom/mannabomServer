package mannabom_server.manabom.domain.user.enums;

import lombok.Getter;

@Getter
public enum RelationshipChoice {
    // 갈등 해결 방식
    IMMEDIATE_RESOLVE("바로 풀고 싶다"),
    TAKE_TIME("시간을 좀 가지고 싶다"),

    // 사진 공유 방식
    SNS_SHARE_OK("SNS에 공유해도 된다"),
    PRIVATE_MEMORY("SNS에 공유하긴 싫다"),

    // 연애에서 중요한 것
    COMFORT("편안함"),
    EXCITEMENT("설렘"),

    // 연인과의 데이트
    INDOOR("실내에서 데이트하기"),
    OUTDOOR("실외에서 데이트하기"),

    // 질투에 대한 태도
    MODERATE_JEALOUSY("적당한 질투가 있어야 재미있다"),
    COOL_ATTITUDE("질투 없이 쿨한 게 편하다"),

    // 이상적인 하루
    COMFORTABLE_DAILY("편안한 일상 즐기기"),
    NEW_EXPERIENCE("새로운 경험 해보기"),

    // 끌리는 점
    CONSIDERATION("배려심 넘치는 모습"),
    STRONG_OPINION("주도적인 모습"),

    // 연인 친구와의 관계
    MIX_WELL("자연스럽게 잘 어울렸으면"),
    SEPARATE_CIRCLE("따로 놀기");

    private final String description;

    RelationshipChoice(String description) {
        this.description = description;
    }
}
