package mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoGiftbizOrderRequest(
        @JsonProperty("template_token") String templateToken,
        @JsonProperty("receiver_type") String receiverType,
        List<Receiver> receivers,
        @JsonProperty("success_callback_url") String successCallbackUrl,
        @JsonProperty("fail_callback_url") String failCallbackUrl,
        @JsonProperty("gift_callback_url") String giftCallbackUrl,
        @JsonProperty("external_order_id") String externalOrderId
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Receiver(
            @JsonProperty("receiver_id") String receiverId,
            String name,
            @JsonProperty("external_key") String externalKey,
            @JsonProperty("sender_name") String senderName,
            @JsonProperty("mc_text") String messageCardText
    ) {
    }
}
