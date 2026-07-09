package mannabom_server.manabom.domain.payment.enums;

public enum PaymentProvider {
    GOOGLE("구글 인앱결제"),
    APPLE("애플 인앱결제"),
    TOSS("토스 PG사");

    private final String description;

    PaymentProvider(String description) {
        this.description = description;
    }
}
