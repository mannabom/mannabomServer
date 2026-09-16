package mannabom_server.manabom.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatMessageType {
    TEXT((short)0), //텍스트
    IMAGE((short)1),  //이미지
    SYSTEM((short)2),
    GIFTICON((short)3); //발송 요청이 접수된 기프티콘

    private final short code;

    public static ChatMessageType from(short code){
        for(var v: values()) if(v.code== code) return v;
        throw new IllegalArgumentException("Invalid ChatMessageType: "+code);
    }

    public String getDisplayMessage(String content){
        return switch(this){
            case IMAGE -> "📷 사진을 보냈습니다.";
            case GIFTICON -> "🎁 기프티콘을 보냈습니다.";
            case TEXT,SYSTEM-> content;

        };
    }
}
