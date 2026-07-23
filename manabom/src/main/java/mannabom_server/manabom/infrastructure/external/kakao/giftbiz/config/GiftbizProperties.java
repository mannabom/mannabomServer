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

    @Getter
    @Setter
    public static class Sync {
        private boolean enabled = true;
        private long initialDelay = 10_000L;
        private long fixedDelay = 1_800_000L;
        private int maxPages = 100;
    }
}
