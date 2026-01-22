package mannabom_server.manabom.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE((short)0),
    FEMALE((short)1);

    private final short code;

    public static Gender from(short code){
        for(var v:values()) if(v.code==code) return v;
        throw new IllegalArgumentException("성별: 존재하지 않은 성별 코드입니다.");
    }

}
