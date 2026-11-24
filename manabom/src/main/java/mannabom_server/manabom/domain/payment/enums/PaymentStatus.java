package mannabom_server.manabom.domain.payment.enums;

public enum PaymentStatus {
    READY("결제 요청이 생성되어 결제사 승인을 기다리는 상태(서버 주도 결제)"),
    VERIFYING("클라이언트 결제 정보가 접수되어 결제사 검증을 진행하는 상태(스토어 인앱결제)"),
    PAID("결제가 확정된 상태"),
    FAILED("결제 검증/승인에 실패한 상태"),
    REFUND_PENDING("환불 요청이 등록되어 처리를 기다리는 상태"),
    REFUND_PROCESSING("결제사에 환불을 요청하고 있는 상태"),
    REFUND_FAILED("환불 시도가 실패하여 재시도 또는 운영 확인이 필요한 상태"),
    REFUNDED("환불이 완료된 상태");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
