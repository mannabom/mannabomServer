package mannabom_server.manabom.application.chat.dto.response;

import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;

public record ChatGifticonInfo(
        Long paymentId,
        Long productId,
        String productName,
        String brandName,
        String productImageUrl,
        String productThumbnailImageUrl
) {
    public static ChatGifticonInfo from(GifticonPayment payment) {
        GifticonProduct product = payment.getProduct();
        return new ChatGifticonInfo(
                payment.getGifticonPaymentId(),
                product.getGifticonProductId(),
                product.getProductName(),
                product.getBrandName(),
                product.getProductImageUrl(),
                product.getProductThumbnailImageUrl()
        );
    }
}
