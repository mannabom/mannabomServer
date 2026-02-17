package mannabom_server.manabom.domain.report.entity;

import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum ReportReason {
    INAPPROPRIATE_PROFILE("부적절한 프로필"),
    ABUSIVE_LANGUAGE("욕설 및 비하 발언"),
    SPAM("스팸/홍보"),
    GHOSTING("잠수"),
    NO_SHOW("노쇼 (약속 장소에 나타나지 않음)"),
    VIOLENT_LANGUAGE("폭언 및 위협"),
    UNCOOPERATIVE("비협조적인 태도"),
    ETC("기타");

    private final String description;

    public ReportReason from(String description){
        for(val v:values()) if(v.description.equals(description)) return v;
        throw new IllegalArgumentException("존재하지 않는 신고 사유 입니다.");
    }
}
