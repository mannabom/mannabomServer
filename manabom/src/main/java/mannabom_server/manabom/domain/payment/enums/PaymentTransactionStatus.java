package mannabom_server.manabom.domain.payment.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaymentTransactionStatus {
    PENDING("결제 검증 전 대기 상태"),
    VERIFY_FAILED("Apple/Google/PG 검증에 실패한 상태"),
    VERIFIED("Apple/Google/PG 검증이 성공한 상태"),
    GRANT_FAILED("검증은 성공했지만 혜택 지급에 실패한 상태"),
    GRANTED("검증된 결제의 혜택 지급까지 완료된 상태"),
    CANCELED("사용자 취소 또는 결제 취소 상태"),
    REFUNDED("환불이 완료되었거나 환불 이벤트가 반영된 상태");

    private final String description;
}
