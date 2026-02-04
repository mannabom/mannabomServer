package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetingDecision {
    WAITING(0),
    ACCEPTED(1),
    REJECTED(2),
    AUTO_ACCEPTED(3),
    AUTO_REJECTED(4);

    private final int code;

    public static MeetingDecision getCode(int code){
        for(var v:values()) if(v.code==code) return v;
        throw new IllegalArgumentException("존재하지 않는 decision 값입니다.");
    }

}
