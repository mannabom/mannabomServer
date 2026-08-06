package mannabom_server.manabom.application.admin.enums;

public enum GifticonPaymentAttentionReason {
    REFUND_FAILED("환불 요청이 실패하여 운영자 확인이 필요합니다."),
    REFUND_PROCESSING_STALE("환불 처리가 제한 시간을 넘겨 멈춰 있습니다."),
    REFUND_ATTEMPTS_EXHAUSTED("자동 환불 최대 시도 횟수를 소진했습니다."),
    MESSAGE_CREATION_STALE("결제 후 메시지가 장시간 생성되지 않았습니다."),
    MESSAGE_CREATION_FAILED_WITHOUT_REFUND("메시지 생성이 실패했지만 환불이 시작되지 않았습니다."),
    PAID_WITHOUT_MESSAGE("결제는 완료됐지만 연결된 메시지 요청이 없습니다."),
    TOSS_STATUS_MISMATCH("애플리케이션 결제 상태와 토스 결제 상태가 일치하지 않습니다.");

    private final String description;

    GifticonPaymentAttentionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
