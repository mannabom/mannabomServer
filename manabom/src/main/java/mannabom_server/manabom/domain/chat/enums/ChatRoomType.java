package mannabom_server.manabom.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRoomType {
    DM_PROFILE((short)0), //일대일 채팅
    DM_CODE((short)1),
    MEETING_GROUP((short)2), //미팅 동성 채팅
    MEETING_MATCH((short)3);  //미팅 이성 채팅(매칭된 이후)

    private final short code;

    public static ChatRoomType from(short code){
        for(var v: values()) if(v.code== code) return v;
        throw new IllegalArgumentException("Invalid ChatRoomType: "+code);
    }
}
