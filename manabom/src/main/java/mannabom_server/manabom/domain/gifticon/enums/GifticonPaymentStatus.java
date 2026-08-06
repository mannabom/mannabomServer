package mannabom_server.manabom.domain.gifticon.enums;

public enum GifticonPaymentStatus {
    READY("결제 요청이 생성되어 구매자 인증을 기다리는 상태"),
    CONFIRMING("토스페이먼츠에 결제 승인을 요청하고 있는 상태"),
    PAID("결제 승인이 완료되어 메시지 요청에 사용할 수 있는 상태"),
    REFUND_PENDING("환불 요청이 등록되어 처리를 기다리는 상태"),
    REFUND_PROCESSING("토스페이먼츠에 결제 취소를 요청하고 있는 상태"),
    REFUNDED("결제 취소와 환불이 완료된 상태"),
    REFUND_FAILED("환불 시도가 실패하여 재시도 또는 운영 확인이 필요한 상태");

    private final String description;

    GifticonPaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
