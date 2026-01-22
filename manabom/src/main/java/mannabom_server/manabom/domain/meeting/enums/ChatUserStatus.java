package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatUserStatus {
    ACTIVE((short)0),
    KICKED((short)1),
    DEACTIVATED((short)2);

    private final short code;

    public static ChatUserStatus from(short code){
        for(var v: ChatUserStatus.values()) if(v.code==code) return v;
        throw new IllegalArgumentException("채팅방유저 상태: 존재하지 않은 유저 상태입니다. " + code);
    }
}
