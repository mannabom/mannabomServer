package mannabom_server.manabom.domain.messageRequest.entity;

import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageRequestGiftPaymentTest {

    @Test
    void holdsAndCapturesPaidTingForGifticon() throws Exception {
        MessageRequest request = giftMessageRequest(3);

        assertThat(request.getHeldGiftTing()).isEqualTo(3);
        assertThat(request.getGiftPaymentStatus()).isEqualTo(GifticonPaymentStatus.HELD);

        request.captureGiftPayment();

        assertThat(request.getGiftPaymentStatus()).isEqualTo(GifticonPaymentStatus.CAPTURED);
        assertThatThrownBy(request::releaseGiftPayment)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releasesExactlyHeldPaidTingWhenRejected() throws Exception {
        MessageRequest request = giftMessageRequest(100);

        int released = request.releaseGiftPayment();

        assertThat(released).isEqualTo(100);
        assertThat(request.getGiftPaymentStatus()).isEqualTo(GifticonPaymentStatus.RELEASED);
    }

    private MessageRequest giftMessageRequest(int tingPrice) throws Exception {
        GifticonProduct product = new GifticonProduct(100L);
        Field tingPriceField = GifticonProduct.class.getDeclaredField("tingPrice");
        tingPriceField.setAccessible(true);
        tingPriceField.setInt(product, tingPrice);
        return new MessageRequest(
                1L,
                2L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH,
                product
        );
    }
}
