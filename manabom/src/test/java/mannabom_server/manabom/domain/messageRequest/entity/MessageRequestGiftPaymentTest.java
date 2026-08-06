package mannabom_server.manabom.domain.messageRequest.entity;

import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRequestGiftPaymentTest {

    @Test
    void attachesOnlyPaidTossPaymentToGifticonMessage() {
        GifticonProduct product = product(11_000);
        GifticonPayment payment = paidPayment(product);
        MessageRequest request = request(product);

        payment.attachTo(request);

        assertThat(request.hasPaidGifticonPayment()).isTrue();
        assertThat(payment.getMessageRequest()).isEqualTo(request);
        assertThat(payment.getAmount()).isEqualTo(11_000);
    }

    @Test
    void rejectedGifticonPaymentMovesToRefundPendingInsteadOfTingRelease() {
        GifticonPayment payment = paidPayment(product(11_000));

        payment.requestRefund();

        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.REFUND_PENDING);
    }

    private GifticonPayment paidPayment(GifticonProduct product) {
        GifticonPayment payment = new GifticonPayment(
                1L,
                product,
                "GIFTICON_123456",
                "CUSTOMER_123456",
                100L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
        payment.startConfirmation(
                "payment-key",
                Instant.now(),
                Instant.now().minusSeconds(60)
        );
        payment.markPaid(Instant.now());
        return payment;
    }

    private MessageRequest request(GifticonProduct product) {
        return new MessageRequest(
                1L,
                2L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH,
                product
        );
    }

    private GifticonProduct product(int salePrice) {
        GifticonProduct product = new GifticonProduct(100L);
        ReflectionTestUtils.setField(product, "salePrice", salePrice);
        return product;
    }
}
