package mannabom_server.manabom.domain.gifticon.enums;

public enum GifticonMessageCreationStatus {
    PENDING("결제 승인 후 메시지 생성을 기다리는 상태"),
    PROCESSING("백엔드가 메시지를 생성하고 있는 상태"),
    RETRY_PENDING("일시적인 오류로 메시지 생성 재시도를 기다리는 상태"),
    CREATED("메시지가 생성되어 결제와 연결된 상태"),
    FAILED("메시지를 생성할 수 없어 결제 환불이 필요한 상태");

    private final String description;

    GifticonMessageCreationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
