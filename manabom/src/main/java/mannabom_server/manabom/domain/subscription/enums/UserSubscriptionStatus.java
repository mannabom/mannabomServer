package mannabom_server.manabom.domain.subscription.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserSubscriptionStatus {
    ACTIVE("구독이 활성 상태이며 혜택을 사용할 수 있음"),
    CANCELED("구독 해지는 되었지만 만료 시각 전까지 혜택을 사용할 수 있음"),
    EXPIRED("구독 기간이 만료되어 혜택을 사용할 수 없음"),
    ON_HOLD("결제 문제로 구독이 보류되어 혜택을 사용할 수 없음");

    private final String description;
}
