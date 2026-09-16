package mannabom_server.manabom.domain.gifticon.enums;

public enum GifticonPaymentPurpose {
    MESSAGE_REQUEST("첫 메시지 요청에 첨부하는 기프티콘 결제"),
    CHAT("이미 열린 1대1 채팅방에서 보내는 기프티콘 결제");

    private final String description;

    GifticonPaymentPurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
