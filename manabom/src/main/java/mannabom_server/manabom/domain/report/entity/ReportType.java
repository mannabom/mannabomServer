package mannabom_server.manabom.domain.report.entity;

import lombok.val;

public enum ReportType {
    CHAT,   //일대일 채팅 프로필 매칭, 러브뷰 // 이성매칭 - 노쇼
    PROFILE; //프로필 조회에서 신고


    public static ReportType from(String value){
        for(val v:values()) if(v.name().equals(value)) return v;
        throw new IllegalArgumentException("존재 하지 않는 신고 타입입니다.");
    }
}
