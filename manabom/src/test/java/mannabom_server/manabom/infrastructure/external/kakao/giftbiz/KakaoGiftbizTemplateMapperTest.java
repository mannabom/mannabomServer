package mannabom_server.manabom.infrastructure.external.kakao.giftbiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizTemplatePage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoGiftbizTemplateMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KakaoGiftbizTemplateMapper mapper = new KakaoGiftbizTemplateMapper();

    @Test
    void mapTemplateWithoutOptionalSalesPeriod() throws Exception {
        String json = """
                {
                  "contents": [
                    {
                      "template_name": "스타벅스-상품권",
                      "template_trace_id": 2026072321334436400,
                      "order_template_status": "ALIVE",
                      "budget_type": "UNLIMITED",
                      "gift_sent_count": 0,
                      "bm_sender_name": "만나봄",
                      "mc_image_url": "https://example.com/card.jpg",
                      "mc_text": "마음이 도착했어요",
                      "product": {
                        "item_type": "VOUCHER",
                        "product_name": "e카드 1만원 교환권",
                        "brand_name": "스타벅스",
                        "product_image_url": "https://example.com/product.jpg",
                        "product_thumb_image_url": "https://example.com/thumb.jpg",
                        "brand_image_url": "https://example.com/brand.jpg",
                        "product_price": 10000
                      }
                    }
                  ],
                  "last": true,
                  "totalCount": 1
                }
                """;
        KakaoGiftbizTemplatePage response =
                objectMapper.readValue(json, KakaoGiftbizTemplatePage.class);

        GifticonTemplateSnapshot result = mapper.toSnapshot(response.contents().get(0));

        assertThat(result.startAt()).isNull();
        assertThat(result.endAt()).isNull();
        assertThat(result.brandName()).isEqualTo("스타벅스");
        assertThat(result.productPrice()).isEqualTo(10_000);
    }

    @Test
    void rejectsTemplateWithoutProductPrice() throws Exception {
        KakaoGiftbizTemplatePage response = objectMapper.readValue(
                """
                {
                  "contents": [{
                    "template_name": "가격 누락",
                    "template_trace_id": 1,
                    "order_template_status": "ALIVE",
                    "product": {"product_name": "상품"}
                  }],
                  "last": true
                }
                """,
                KakaoGiftbizTemplatePage.class
        );

        assertThatThrownBy(() -> mapper.toSnapshot(response.contents().get(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 가격");
    }
}
