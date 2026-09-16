package mannabom_server.manabom.infrastructure.external.kakao.giftbiz;

import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoGiftbizOrderClientTest {

    @Test
    void combinesConfiguredSenderNameAndProfileNickname() {
        GiftbizProperties properties = new GiftbizProperties();
        properties.setSenderName("만나봄");
        KakaoGiftbizOrderClient client = new KakaoGiftbizOrderClient(properties);

        assertThat(client.formatSenderName("봄이"))
                .isEqualTo("만나봄 - 봄이");
    }
}
