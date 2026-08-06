package mannabom_server.manabom.infrastructure.external.toss;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void confirmsPaymentWithBasicAuthAndServerAmount() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer("/v1/payments/confirm", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            requestBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            respond(exchange, """
                    {
                      "paymentKey":"payment-key",
                      "orderId":"GIFTICON_123456",
                      "totalAmount":11100,
                      "status":"DONE"
                    }
                    """);
        });

        GifticonPaymentGateway.PaymentResult result = client().confirm(
                new GifticonPaymentGateway.ConfirmPaymentCommand(
                        "payment-key",
                        "GIFTICON_123456",
                        11_100,
                        "confirm-idempotency"
                )
        );

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(authorization.get()).isEqualTo(
                "Basic " + Base64.getEncoder().encodeToString(
                        "test_sk:".getBytes(StandardCharsets.UTF_8)
                )
        );
        assertThat(idempotencyKey.get()).isEqualTo("confirm-idempotency");
        assertThat(requestBody.get())
                .contains("\"paymentKey\":\"payment-key\"")
                .contains("\"orderId\":\"GIFTICON_123456\"")
                .contains("\"amount\":11100");
    }

    @Test
    void cancelsPaymentWithStableIdempotencyKey() throws Exception {
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer("/v1/payments/payment-key/cancel", exchange -> {
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            requestBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            respond(exchange, """
                    {
                      "paymentKey":"payment-key",
                      "orderId":"GIFTICON_123456",
                      "totalAmount":11100,
                      "status":"CANCELED"
                    }
                    """);
        });

        GifticonPaymentGateway.PaymentResult result = client().cancel(
                new GifticonPaymentGateway.CancelPaymentCommand(
                        "payment-key",
                        "메시지 요청 거절",
                        "refund-idempotency"
                )
        );

        assertThat(result.status()).isEqualTo("CANCELED");
        assertThat(idempotencyKey.get()).isEqualTo("refund-idempotency");
        assertThat(requestBody.get()).contains("메시지 요청 거절");
    }

    @Test
    void getsPaymentByPaymentKeyForAdminReconciliation() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        startServer("/v1/payments/payment-key", exchange -> {
            method.set(exchange.getRequestMethod());
            respond(exchange, """
                    {
                      "paymentKey":"payment-key",
                      "orderId":"GIFTICON_123456",
                      "totalAmount":11100,
                      "status":"DONE"
                    }
                    """);
        });

        GifticonPaymentGateway.PaymentResult result = client().getPayment(
                new GifticonPaymentGateway.PaymentLookup(
                        "payment-key",
                        "GIFTICON_123456"
                )
        );

        assertThat(method.get()).isEqualTo("GET");
        assertThat(result.status()).isEqualTo("DONE");
    }

    private TossPaymentClient client() {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setSecretKey("test_sk");
        properties.setRequestTimeoutSeconds(2);
        return new TossPaymentClient(properties);
    }

    private void startServer(
            String path,
            ThrowingExchangeHandler handler
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ThrowingExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
