package mannabom_server.manabom.infrastructure.external.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway.PaymentLookup;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class TossPaymentClient implements GifticonPaymentGateway {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final TossPaymentsProperties properties;
    private final WebClient webClient;

    public TossPaymentClient(TossPaymentsProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public PaymentResult confirm(ConfirmPaymentCommand command) {
        TossPaymentResponse response = webClient.post()
                .uri("/v1/payments/confirm")
                .headers(this::setAuthorization)
                .header(IDEMPOTENCY_KEY, command.idempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "paymentKey", command.paymentKey(),
                        "orderId", command.orderId(),
                        "amount", command.amount()
                ))
                .retrieve()
                .onStatus(status -> status.isError(), client -> client.createException())
                .bodyToMono(TossPaymentResponse.class)
                .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
        return toResult(response);
    }

    @Override
    public PaymentResult cancel(CancelPaymentCommand command) {
        TossPaymentResponse response = webClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", command.paymentKey())
                .headers(this::setAuthorization)
                .header(IDEMPOTENCY_KEY, command.idempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("cancelReason", command.cancelReason()))
                .retrieve()
                .onStatus(status -> status.isError(), client -> client.createException())
                .bodyToMono(TossPaymentResponse.class)
                .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
        return toResult(response);
    }

    @Override
    public PaymentResult getPayment(PaymentLookup query) {
        if (StringUtils.hasText(query.paymentKey())) {
            return get("/v1/payments/{identifier}", query.paymentKey());
        }
        if (StringUtils.hasText(query.orderId())) {
            return get("/v1/payments/orders/{identifier}", query.orderId());
        }
        throw new IllegalArgumentException("토스 결제 조회 식별자가 필요합니다.");
    }

    private PaymentResult get(String uri, String identifier) {
        TossPaymentResponse response = webClient.get()
                .uri(uri, identifier)
                .headers(this::setAuthorization)
                .retrieve()
                .onStatus(status -> status.isError(), client -> client.createException())
                .bodyToMono(TossPaymentResponse.class)
                .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
        return toResult(response);
    }

    private void setAuthorization(HttpHeaders headers) {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("TOSS_PAYMENTS_SECRET_KEY 설정이 필요합니다.");
        }
        if (properties.getRequestTimeoutSeconds() <= 0) {
            throw new IllegalStateException("토스페이먼츠 요청 제한 시간은 1초 이상이어야 합니다.");
        }
        headers.setBasicAuth(properties.getSecretKey().trim(), "");
    }

    private PaymentResult toResult(TossPaymentResponse response) {
        if (response == null) {
            throw new IllegalStateException("토스페이먼츠가 빈 응답을 반환했습니다.");
        }
        return new PaymentResult(
                response.paymentKey(),
                response.orderId(),
                response.totalAmount(),
                response.status()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossPaymentResponse(
            String paymentKey,
            String orderId,
            int totalAmount,
            String status
    ) {
    }
}
