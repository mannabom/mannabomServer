package mannabom_server.manabom.infrastructure.external.toss.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.toss-payments")
public class TossPaymentsProperties {

    private String baseUrl = "https://api.tosspayments.com";
    private String clientKey;
    private String secretKey;
    private int requestTimeoutSeconds = 10;
    private MessageCreationRetry messageCreationRetry = new MessageCreationRetry();
    private RefundRetry refundRetry = new RefundRetry();
    private UnusedPaymentRefund unusedPaymentRefund = new UnusedPaymentRefund();

    @Getter
    @Setter
    public static class RefundRetry {
        private boolean enabled = true;
        private long initialDelay = 60_000L;
        private long fixedDelay = 60_000L;
        private int maxAttempts = 10;
        private int batchSize = 50;
    }

    @Getter
    @Setter
    public static class MessageCreationRetry {
        private boolean enabled = true;
        private long initialDelay = 60_000L;
        private long fixedDelay = 60_000L;
        private int maxAttempts = 10;
        private int batchSize = 50;
    }

    @Getter
    @Setter
    public static class UnusedPaymentRefund {
        private boolean enabled = true;
        private long gracePeriodMinutes = 30L;
        private long initialDelay = 60_000L;
        private long fixedDelay = 60_000L;
        private int batchSize = 50;
    }
}
