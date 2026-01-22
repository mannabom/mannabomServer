package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    LEADER((short)1),
    MEMBER((short)0),
    KICKED((short)3),
    DEACTIVATED((short)4);

    private final short code;

    public static Role from(short code){
        for(var v:Role.values()) if(v.code==code) return v;
        throw new IllegalArgumentException("미팅방 역할: 존재하지 않은 역할입니다. " + code);
    }

}
