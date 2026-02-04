package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetingRole {
    LEADER(1),
    MEMBER(0),
    KICKED(3),
    DEACTIVATED(4);

    private final int code;

    public static MeetingRole from(int code){
        for(var v: MeetingRole.values()) if(v.code==code) return v;
        throw new IllegalArgumentException("미팅방 역할: 존재하지 않은 역할입니다. " + code);
    }

}
