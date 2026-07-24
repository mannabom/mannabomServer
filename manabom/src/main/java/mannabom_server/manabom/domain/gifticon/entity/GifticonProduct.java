package mannabom_server.manabom.domain.gifticon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "gifticon_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GifticonProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gifticon_product_id")
    private Long gifticonProductId;

    @Column(name = "template_trace_id", nullable = false, unique = true)
    private Long templateTraceId;

    @Column(name = "template_token", unique = true, length = 512)
    private String templateToken;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "order_template_status", nullable = false, length = 30)
    private String orderTemplateStatus;

    @Column(name = "budget_type", length = 30)
    private String budgetType;

    @Column(name = "gift_sent_count", nullable = false)
    private long giftSentCount;

    @Column(name = "bm_sender_name", length = 100)
    private String businessMessageSenderName;

    @Column(name = "mc_image_url", length = 2048)
    private String messageCardImageUrl;

    @Column(name = "mc_text", columnDefinition = "TEXT")
    private String messageCardText;

    @Column(name = "item_type", nullable = false, length = 30)
    private String itemType;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "product_image_url", length = 2048)
    private String productImageUrl;

    @Column(name = "product_thumb_image_url", length = 2048)
    private String productThumbnailImageUrl;

    @Column(name = "brand_image_url", length = 2048)
    private String brandImageUrl;

    @Column(name = "product_price", nullable = false)
    private int productPrice;

    @Column(name = "ting_price", nullable = false)
    private int tingPrice;

    @Column(name = "ting_price_manually_set", nullable = false)
    private boolean tingPriceManuallySet;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    public GifticonProduct(Long templateTraceId) {
        this.templateTraceId = templateTraceId;
    }

    public void synchronize(
            String templateName,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String orderTemplateStatus,
            String budgetType,
            long giftSentCount,
            String businessMessageSenderName,
            String messageCardImageUrl,
            String messageCardText,
            String itemType,
            String productName,
            String brandName,
            String productImageUrl,
            String productThumbnailImageUrl,
            String brandImageUrl,
            int productPrice,
            int calculatedTingPrice,
            Instant syncedAt
    ) {
        this.templateName = templateName;
        this.startAt = startAt;
        this.endAt = endAt;
        this.orderTemplateStatus = orderTemplateStatus;
        this.budgetType = budgetType;
        this.giftSentCount = giftSentCount;
        this.businessMessageSenderName = businessMessageSenderName;
        this.messageCardImageUrl = messageCardImageUrl;
        this.messageCardText = messageCardText;
        this.itemType = itemType;
        this.productName = productName;
        this.brandName = brandName;
        this.productImageUrl = productImageUrl;
        this.productThumbnailImageUrl = productThumbnailImageUrl;
        this.brandImageUrl = brandImageUrl;
        this.productPrice = productPrice;
        if (!this.tingPriceManuallySet) {
            this.tingPrice = calculatedTingPrice;
        }
        this.available = "ALIVE".equals(orderTemplateStatus);
        this.lastSyncedAt = syncedAt;
    }

    public void changeTingPrice(int tingPrice) {
        if (tingPrice <= 0) {
            throw new IllegalArgumentException("팅 판매가는 0보다 커야 합니다.");
        }
        this.tingPrice = tingPrice;
        this.tingPriceManuallySet = true;
    }

    public void useCalculatedTingPrice(int calculatedTingPrice) {
        if (calculatedTingPrice < 0) {
            throw new IllegalArgumentException("계산된 팅 판매가는 0 이상이어야 합니다.");
        }
        this.tingPrice = calculatedTingPrice;
        this.tingPriceManuallySet = false;
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return available
                && (startAt == null || !startAt.isAfter(now))
                && (endAt == null || endAt.isAfter(now));
    }

    public boolean isOrderableAt(LocalDateTime now) {
        return hasTemplateToken() && isAvailableAt(now);
    }

    public boolean hasTemplateToken() {
        return templateToken != null && !templateToken.isBlank();
    }

    public void configureTemplateToken(String templateToken) {
        if (templateToken == null || templateToken.isBlank()) {
            throw new IllegalArgumentException("템플릿 토큰은 비어있을 수 없습니다.");
        }
        this.templateToken = templateToken.trim();
    }
}
