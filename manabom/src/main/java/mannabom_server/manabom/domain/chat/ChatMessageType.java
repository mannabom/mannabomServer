package mannabom_server.manabom.domain.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatMessageType {
    TEXT((short)0), //텍스트
    IMAGE((short)1);  //이미지

    private final short code;

    public static ChatMessageType from(short code){
        for(var v: values()) if(v.code== code) return v;
        throw new IllegalArgumentException("Invalid ChatMessageType: "+code);
    }
}
