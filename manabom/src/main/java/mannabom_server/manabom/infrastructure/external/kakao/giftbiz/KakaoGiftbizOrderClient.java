package mannabom_server.manabom.infrastructure.external.kakao.giftbiz;

import mannabom_server.manabom.application.gifticon.port.GifticonOrderRequester;
import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizOrderRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class KakaoGiftbizOrderClient implements GifticonOrderRequester {

    private static final String ORDER_PATH = "/openapi/giftbiz/v1/template/order";
    private static final String RECEIVER_TYPE_PHONE = "PHONE";

    private final GiftbizProperties properties;
    private final WebClient webClient;

    public KakaoGiftbizOrderClient(GiftbizProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public void requestGift(GifticonOrderCommand command) {
        validateConfiguration();
        GiftbizProperties.Order orderProperties = properties.getOrder();
        KakaoGiftbizOrderRequest request = new KakaoGiftbizOrderRequest(
                command.templateToken(),
                RECEIVER_TYPE_PHONE,
                List.of(new KakaoGiftbizOrderRequest.Receiver(
                        command.receiverPhone(),
                        command.receiverName(),
                        command.externalKey(),
                        properties.getSenderName(),
                        properties.getText()
                )),
                blankToNull(orderProperties.getSuccessCallbackUrl()),
                blankToNull(orderProperties.getFailCallbackUrl()),
                blankToNull(orderProperties.getGiftCallbackUrl()),
                command.externalOrderId()
        );

        webClient.post()
                .uri(ORDER_PATH)
                .header(HttpHeaders.AUTHORIZATION, properties.getAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.createException()
                )
                .toBodilessEntity()
                .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getAuthorization())) {
            throw new IllegalStateException("KAKAO_GIFTBIZ_AUTHORIZATION 설정이 필요합니다.");
        }
        if (!StringUtils.hasText(properties.getSenderName())) {
            throw new IllegalStateException("KAKAO_GIFTBIZ_SENDER_NAME 설정이 필요합니다.");
        }
        if (properties.getRequestTimeoutSeconds() <= 0) {
            throw new IllegalStateException("Gift Biz 요청 제한 시간은 1초 이상이어야 합니다.");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
