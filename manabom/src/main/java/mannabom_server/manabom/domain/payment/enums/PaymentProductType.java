package mannabom_server.manabom.domain.payment.enums;

public enum PaymentProductType {
    TING("팅 재화"),
    SUBSCRIPTION("구독권"),
    GIFTICON("기프티콘");

    private final String description;

    PaymentProductType(String description){
        this.description = description;
    }
}
