package mannabom_server.manabom.domain.currency.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TingWalletHistoryReason {
    TING_CHARGE("인앱결제를 통한 팅 충전"),
    PROFILE_REQUEST("프로필 요청에 팅 사용"),
    LOVE_MESSAGE("호감 메시지 발송에 팅 사용"),
    EXTRA_PROFILE("추가 프로필 조회에 팅 사용"),
    SUBSCRIPTION_BENEFIT("정기구독 혜택 지급 또는 사용"),
    PAYMENT_REFUND("결제 환불 처리"),
    ADMIN_ADJUST("관리자 수동 조정");

    private final String description;
}