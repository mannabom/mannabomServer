package mannabom_server.manabom.domain.payment.enums;

public enum PaymentProvider {
    APPLE_IAP("Apple App Store 인앱결제"),
    GOOGLE_IAP("Google Play 인앱결제"),
    TOSS("토스페이먼츠");

    private final String description;

    PaymentProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
