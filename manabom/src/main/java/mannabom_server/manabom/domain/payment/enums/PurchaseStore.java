package mannabom_server.manabom.domain.payment.enums;

public enum PurchaseStore {
    APPLE(PaymentProvider.APPLE_IAP, "Apple App Store"),
    GOOGLE(PaymentProvider.GOOGLE_IAP, "Google Play");

    private final PaymentProvider provider;
    private final String description;

    PurchaseStore(PaymentProvider provider, String description) {
        this.provider = provider;
        this.description = description;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public String getDescription() {
        return description;
    }
}
