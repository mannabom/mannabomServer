package mannabom_server.manabom.domain.gifticon.vo;

import java.time.LocalDateTime;

public record GifticonTemplateSnapshot(
        Long templateTraceId,
        String templateName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String budgetType,
        Long sentCount,
        String senderName,
        String messageCardImageUrl,
        String messageCardText,
        String itemType,
        String productName,
        String brandName,
        String productImageUrl,
        String productThumbnailImageUrl,
        String brandImageUrl,
        Integer productPrice
) {
}
