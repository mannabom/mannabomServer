package mannabom_server.manabom.application.admin.dto.response;

import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;

public record AdminGifticonProductResponse(
        Long gifticonProductId,
        String templateTraceId,
        String templateName,
        String productName,
        String brandName,
        int productPrice,
        int tingPrice,
        boolean available,
        boolean templateTokenConfigured
) {
    public static AdminGifticonProductResponse from(GifticonProduct product) {
        return new AdminGifticonProductResponse(
                product.getGifticonProductId(),
                String.valueOf(product.getTemplateTraceId()),
                product.getTemplateName(),
                product.getProductName(),
                product.getBrandName(),
                product.getProductPrice(),
                product.getTingPrice(),
                product.isAvailable(),
                product.hasTemplateToken()
        );
    }
}
