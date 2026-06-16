package mannabom_server.manabom.domain.report.entity;

import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum ReportStatus {
    RECEIVED("접수됨"),     // 신고가 막 들어온 상태
    UNDER_REVIEW("검토 중"), // 관리자가 확인 중인 상태
    RESOLVED("처리 완료"),   // 제재를 가하거나 조치를 취함
    REJECTED("반려");       // 근거가 부족하여 처리를 하지 않음

    private final String description;

    public ReportStatus from(String description){
        for(val v : values()) if(v.description.equals(description)) return v;
        throw new IllegalArgumentException("존재하지 않는 신고 상태 입니다.");
    }
}
