package mannabom_server.manabom.application.gifticon.dto.response;

import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;

import java.time.LocalDateTime;

public record GifticonProductResponse(
        Long gifticonProductId,
        String templateTraceId,
        String templateName,
        String itemType,
        String productName,
        String brandName,
        String productImageUrl,
        String productThumbnailImageUrl,
        String brandImageUrl,
        int productPrice,
        int tingPrice,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static GifticonProductResponse from(GifticonProduct product) {
        return new GifticonProductResponse(
                product.getGifticonProductId(),
                String.valueOf(product.getTemplateTraceId()),
                product.getTemplateName(),
                product.getItemType(),
                product.getProductName(),
                product.getBrandName(),
                product.getProductImageUrl(),
                product.getProductThumbnailImageUrl(),
                product.getBrandImageUrl(),
                product.getProductPrice(),
                product.getTingPrice(),
                product.getStartAt(),
                product.getEndAt()
        );
    }
}
