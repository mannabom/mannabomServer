package mannabom_server.manabom.domain.payment.enums;

public enum PaymentPurpose {
    TING_CHARGE("팅 충전"),
    GIFTICON("기프티콘 선물");

    private final String description;

    PaymentPurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
