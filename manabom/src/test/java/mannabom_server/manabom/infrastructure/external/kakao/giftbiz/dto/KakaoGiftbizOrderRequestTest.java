package mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoGiftbizOrderRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesKakaoOrderFieldNamesAndOmitsEmptyCallbacks() throws Exception {
        KakaoGiftbizOrderRequest request = new KakaoGiftbizOrderRequest(
                "template-token",
                "PHONE",
                List.of(new KakaoGiftbizOrderRequest.Receiver(
                        "01012345678",
                        "수신자",
                        "MESSAGE-GIFT-1-2",
                        "만나봄",
                        "마음이 도착했어요"
                )),
                null,
                null,
                null,
                "MESSAGE-GIFT-1"
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(request));

        assertThat(json.get("template_token").asText()).isEqualTo("template-token");
        assertThat(json.get("receiver_type").asText()).isEqualTo("PHONE");
        assertThat(json.get("external_order_id").asText()).isEqualTo("MESSAGE-GIFT-1");
        assertThat(json.has("success_callback_url")).isFalse();
        JsonNode receiver = json.get("receivers").get(0);
        assertThat(receiver.get("receiver_id").asText()).isEqualTo("01012345678");
        assertThat(receiver.get("external_key").asText()).isEqualTo("MESSAGE-GIFT-1-2");
        assertThat(receiver.get("sender_name").asText()).isEqualTo("만나봄");
        assertThat(receiver.get("mc_text").asText()).isEqualTo("마음이 도착했어요");
    }
}
