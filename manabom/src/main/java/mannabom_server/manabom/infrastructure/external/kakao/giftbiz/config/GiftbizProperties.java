package mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.kakao.giftbiz")
public class GiftbizProperties {

    private String baseUrl = "https://gateway-giftbiz.kakao.com";
    private String authorization;
    private String senderName = "만나봄";
    private String text = "";
    private int requestTimeoutSeconds = 10;
    private Sync sync = new Sync();
    private Order order = new Order();

    @Getter
    @Setter
    public static class Sync {
        private boolean enabled = true;
        private long initialDelay = 10_000L;
        private long fixedDelay = 1_800_000L;
        private int maxPages = 100;
    }

    @Getter
    @Setter
    public static class Order {
        private String successCallbackUrl;
        private String failCallbackUrl;
        private String giftCallbackUrl;
        private Retry retry = new Retry();
    }

    @Getter
    @Setter
    public static class Retry {
        private boolean enabled = true;
        private long initialDelay = 60_000L;
        private long fixedDelay = 60_000L;
        private int maxAttempts = 5;
        private int batchSize = 50;
    }
}
