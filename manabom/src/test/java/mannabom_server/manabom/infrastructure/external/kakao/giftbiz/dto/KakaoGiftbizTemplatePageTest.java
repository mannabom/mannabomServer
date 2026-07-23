package mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoGiftbizTemplatePageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializeTemplateListResponse() throws Exception {
        String json = """
                {
                  "contents": [
                    {
                      "template_name": "편의점-츄파츕스",
                      "template_trace_id": 2026072304113962800,
                      "start_at": "20260723000000",
                      "end_at": "20260801000000",
                      "order_template_status": "ALIVE",
                      "budget_type": "UNLIMITED",
                      "gift_sent_count": 0,
                      "bm_sender_name": "만나봄",
                      "mc_image_url": "https://example.com/card.jpg",
                      "mc_text": "테스트",
                      "product": {
                        "item_type": "VOUCHER",
                        "product_name": "퍼페티)츄파춥스",
                        "brand_name": "GS25",
                        "product_image_url": "https://example.com/product.png",
                        "product_thumb_image_url": "https://example.com/thumb.png",
                        "brand_image_url": "https://example.com/brand.png",
                        "product_price": 300
                      }
                    }
                  ],
                  "last": true,
                  "totalCount": 1
                }
                """;

        KakaoGiftbizTemplatePage result =
                objectMapper.readValue(json, KakaoGiftbizTemplatePage.class);

        assertThat(result.last()).isTrue();
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.contents()).hasSize(1);
        assertThat(result.contents().get(0).templateTraceId())
                .isEqualTo(2026072304113962800L);
        assertThat(result.contents().get(0).product().productPrice()).isEqualTo(300);
    }
}
