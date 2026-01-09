package mannabom_server.manabom.domain.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatStatus {
    ENABLED((short)0), //채팅 가능
    DISABLED((short)1); //채팅 불가능

    private final short code;

    public static ChatStatus from(short code){
        for(var v: values()) if(v.code==code) return v;
        throw new IllegalArgumentException("Invalid code "+ code);
    }
}
