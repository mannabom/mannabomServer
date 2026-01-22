package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MeetingType {
    GENERAL((short)0);

    private final short code;

    public static MeetingType from(short code){
        for(var v: values()) if(v.code == code) return v;
        throw new IllegalArgumentException("미팅 타입: 존재하지 않은 미팅타입 입니다.");
    }


}
