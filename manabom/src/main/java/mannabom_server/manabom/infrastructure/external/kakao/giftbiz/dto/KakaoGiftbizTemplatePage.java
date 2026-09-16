package mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoGiftbizTemplatePage(
        List<KakaoGiftbizTemplate> contents,
        Boolean last,
        @JsonProperty("totalCount") Long totalCount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoGiftbizTemplate(
            @JsonProperty("template_name") String templateName,
            @JsonProperty("template_trace_id") Long templateTraceId,
            @JsonProperty("start_at") String startAt,
            @JsonProperty("end_at") String endAt,
            @JsonProperty("order_template_status") String orderTemplateStatus,
            @JsonProperty("budget_type") String budgetType,
            @JsonProperty("gift_sent_count") Long giftSentCount,
            @JsonProperty("bm_sender_name") String businessMessageSenderName,
            @JsonProperty("mc_image_url") String messageCardImageUrl,
            @JsonProperty("mc_text") String messageCardText,
            Product product
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
            @JsonProperty("item_type") String itemType,
            @JsonProperty("product_name") String productName,
            @JsonProperty("brand_name") String brandName,
            @JsonProperty("product_image_url") String productImageUrl,
            @JsonProperty("product_thumb_image_url") String productThumbnailImageUrl,
            @JsonProperty("brand_image_url") String brandImageUrl,
            @JsonProperty("product_price") Integer productPrice
    ) {
    }
}
