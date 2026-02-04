package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MeetingType {
    GENERAL(0);

    private final int code;

    public static MeetingType from(int code){
        for(var v: values()) if(v.code == code) return v;
        throw new IllegalArgumentException("미팅 타입: 존재하지 않은 미팅타입 입니다.");
    }


}
