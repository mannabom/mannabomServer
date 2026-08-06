package mannabom_server.manabom.infrastructure.external.kakao.giftbiz;

import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizTemplatePage.KakaoGiftbizTemplate;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizTemplatePage.Product;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class KakaoGiftbizTemplateMapper {

    private static final DateTimeFormatter KAKAO_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public GifticonTemplateSnapshot toSnapshot(KakaoGiftbizTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("Gift Biz 템플릿은 null일 수 없습니다.");
        }
        Product product = template.product();
        if (product == null || product.productPrice() == null) {
            throw new IllegalArgumentException("Gift Biz 템플릿의 상품 가격이 누락되었습니다.");
        }
        return new GifticonTemplateSnapshot(
                template.templateTraceId(),
                template.templateName(),
                parseOptionalDateTime(template.startAt(), "start_at"),
                parseOptionalDateTime(template.endAt(), "end_at"),
                template.orderTemplateStatus(),
                template.budgetType(),
                template.giftSentCount(),
                template.businessMessageSenderName(),
                template.messageCardImageUrl(),
                template.messageCardText(),
                product.itemType(),
                product.productName(),
                product.brandName(),
                product.productImageUrl(),
                product.productThumbnailImageUrl(),
                product.brandImageUrl(),
                product.productPrice()
        );
    }

    private LocalDateTime parseOptionalDateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, KAKAO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "Gift Biz 템플릿의 " + fieldName + " 형식이 올바르지 않습니다: " + value,
                    e
            );
        }
    }
}
