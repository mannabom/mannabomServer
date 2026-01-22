package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchingStatus {
    RECRUITING((short)0), //팀원 모집중
    FULL((short)1), //팀 구성 완료
    MATCHING((short)2), // 매칭 대기중
    MATCHED((short)3),
    FASTMATCHING((short)4); // 매칭 완료


    private final short code;

    public static MatchingStatus from(short code){
        for(var v:values()) if(v.code==code) return v;
        throw new IllegalArgumentException("매칭 상태: 존재하지 않는 매칭 타입 코드 값입니다.");
    }

}
