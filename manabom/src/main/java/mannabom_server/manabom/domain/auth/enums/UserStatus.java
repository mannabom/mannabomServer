package mannabom_server.manabom.domain.auth.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("기존 사용자"),                     // 이미 가입된 사용자 - 로그인 성공
    PENDING_VERIFICATION("회원가입 진행"),     // 신규 사용자 - 회원가입 필요
    AGE_RESTRICTED("연령 제한 사용자");        // 20대가 아닌 사용자 - 가입 불가

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }
}
