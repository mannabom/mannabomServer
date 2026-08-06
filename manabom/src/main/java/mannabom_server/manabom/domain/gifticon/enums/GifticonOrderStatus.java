package mannabom_server.manabom.domain.gifticon.enums;

public enum GifticonOrderStatus {
    PENDING("결제 및 발송 조건을 충족하여 외부 기프티콘 발송 요청을 기다리는 상태"),
    PROCESSING("외부 기프티콘 공급사에 발송을 요청하고 있는 상태"),
    REQUESTED("외부 기프티콘 공급사가 발송 요청을 정상 접수한 상태"),
    FAILED("기프티콘 발송 요청이 실패하여 재시도 또는 운영 확인이 필요한 상태");

    private final String description;

    GifticonOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
