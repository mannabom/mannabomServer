package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ting-purchase")
public class TingPurchaseProperties {

    /** 샌드박스/테스트 구매도 팅을 지급할지. Apple 심사와 내부 테스트를 위해 기본 true */
    private boolean allowSandboxCredit = true;
    /** 스토어 영수증의 계정 토큰이 없으면 거절할지. 클라이언트가 항상 토큰을 넣도록 바뀐 뒤 true 로 전환 */
    private boolean requireAccountToken = false;
    /** 스토어 통신 제한 시간(초). PROCESSING 상태를 stale 로 판정하는 기준에도 사용 */
    private int requestTimeoutSeconds = 10;
    private ConsumeRetry consumeRetry = new ConsumeRetry();
    private VoidedPurchaseSync voidedPurchaseSync = new VoidedPurchaseSync();

    @Getter
    @Setter
    public static class ConsumeRetry {
        private boolean enabled = true;
        private long initialDelay = 60_000L;
        private long fixedDelay = 60_000L;
        private int maxAttempts = 10;
        private int batchSize = 50;
    }

    @Getter
    @Setter
    public static class VoidedPurchaseSync {
        private boolean enabled = false;
        private long initialDelay = 120_000L;
        private long fixedDelay = 3_600_000L;
        /** Google 은 최대 30일 이전까지만 조회를 허용한다 */
        private int lookbackDays = 30;
    }
}
