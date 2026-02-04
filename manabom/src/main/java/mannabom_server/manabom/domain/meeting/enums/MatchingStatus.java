package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchingStatus {
    PENDING(0),   // 매칭 성사 후 수락/거절 대기 중 (기본값)
    SUCCEEDED(1), // 최종 매칭 성공 (둘 다 수락)
    FAILED(2);  // 매칭 실패 (한 명이라도 거절)
    private final int code;

    public static MatchingStatus fromCode(int code){
        for(var v: values()) if(v.code == code) return v;
        throw new IllegalArgumentException("존재하지 않는 매칭상태 값입니다.");
    }
}