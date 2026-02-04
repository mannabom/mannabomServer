package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MeetingStatus {
    RECRUITING(0), //팀원 모집중
    FULL(1), //팀 구성 완료
    MATCHING_WAITING(2), //매칭 대기중
    MATCHING_PENDING(3), // 매칭 완료 상대 수락/거절 대기중
    MATCHED(4),  //매칭 성공
    FASTMATCHING(5); // 빠른 매칭


    private final int code;

    public static MeetingStatus from(int code){
        for(var v:values()) if(v.code==code) return v;
        throw new IllegalArgumentException("매칭 상태: 존재하지 않는 매칭 타입 코드 값입니다.");
    }

}
