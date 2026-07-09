package mannabom_server.manabom.domain.currency.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TingWalletHistoryType {
    CHARGE("팅 충전"),
    USE("팅 사용"),
    REFUND("결제 환불로 인한 팅 반환 또는 차감"),
    ADMIN_GRANT("관리자 지급"),
    ADMIN_DEDUCT("관리자 차감");

    private final String description;
}
