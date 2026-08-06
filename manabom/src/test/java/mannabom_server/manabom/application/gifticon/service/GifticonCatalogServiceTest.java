package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.application.gifticon.dto.response.GifticonProductSliceResponse;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.domain.gifticon.service.GifticonPriceCalculator;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GifticonCatalogServiceTest {

    @Mock
    private GifticonProductRepository gifticonProductRepository;

    @Mock
    private RuntimePolicyService runtimePolicyService;

    @Test
    void getAvailableProductsSupportsCategoryCursorPagination() {
        GifticonCatalogService service = service();
        when(gifticonProductRepository.findAvailableProductsAfter(
                any(),
                eq("GS25"),
                eq(10L),
                any(Pageable.class)
        )).thenReturn(List.of(
                product(11L, "GS25"),
                product(12L, "GS25"),
                product(13L, "GS25")
        ));

        GifticonProductSliceResponse result =
                service.getAvailableProducts(" GS25 ", 10L, 2);

        assertThat(result.contents()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(12L);
        verify(gifticonProductRepository).findAvailableProductsAfter(
                any(),
                eq("GS25"),
                eq(10L),
                argThat(pageable -> pageable.getPageSize() == 3)
        );
    }

    @Test
    void getAvailableProductsQueriesAllBrandsWhenCategoryIsNull() {
        GifticonCatalogService service = service();
        when(gifticonProductRepository.findAvailableProductsAfter(
                any(),
                isNull(),
                eq(0L),
                any(Pageable.class)
        )).thenReturn(List.of(product(1L, "GS25")));

        GifticonProductSliceResponse result =
                service.getAvailableProducts(null, null, 20);

        assertThat(result.contents()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void synchronizeAliveTemplatesCreatesProductFromKakaoResponse() {
        GifticonCatalogService service = service();
        GifticonTemplateSnapshot template = template();
        when(runtimePolicyService.snapshot()).thenReturn(RuntimePolicySnapshot.builder()
                .gifticon(RuntimePolicySnapshot.Gifticon.builder()
                        .pricing(RuntimePolicySnapshot.Gifticon.Pricing.builder()
                                .markupPercent(BigDecimal.TEN)
                                .roundUnit(100)
                                .build())
                        .build())
                .build());
        when(gifticonProductRepository.findAllByTemplateTraceIdIn(anyCollection()))
                .thenReturn(List.of());

        int synchronizedCount = service.synchronizeAliveTemplates(List.of(template));

        assertThat(synchronizedCount).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GifticonProduct>> captor = ArgumentCaptor.forClass(List.class);
        verify(gifticonProductRepository).saveAllAndFlush(captor.capture());

        GifticonProduct product = captor.getValue().get(0);
        assertThat(product.getTemplateTraceId()).isEqualTo(2026072304113962800L);
        assertThat(product.getProductName()).isEqualTo("퍼페티)츄파춥스");
        assertThat(product.getBrandName()).isEqualTo("GS25");
        assertThat(product.getProductPrice()).isEqualTo(300);
        assertThat(product.getSalePrice()).isEqualTo(400);
        assertThat(product.getStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 0, 0));
        assertThat(product.isAvailable()).isTrue();
        verify(gifticonProductRepository)
                .markProductsNotSeenSinceUnavailable(product.getLastSyncedAt());
    }

    @Test
    void synchronizeAliveTemplatesStopsBeforeDatabaseWritesWhenResponseIsEmpty() {
        GifticonCatalogService service = service();

        assertThatThrownBy(() -> service.synchronizeAliveTemplates(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("활성 템플릿이 0개이므로 동기화를 중단합니다.");

        verifyNoInteractions(gifticonProductRepository);
    }

    private GifticonCatalogService service() {
        GifticonPriceCalculator priceCalculator = new GifticonPriceCalculator(
                BigDecimal.TEN,
                100
        );
        return new GifticonCatalogService(
                gifticonProductRepository,
                priceCalculator,
                runtimePolicyService
        );
    }

    private GifticonProduct product(Long id, String brandName) {
        GifticonProduct product = new GifticonProduct(id);
        product.synchronize(
                "템플릿-" + id,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0),
                "ALIVE",
                "UNLIMITED",
                0L,
                "만나봄",
                null,
                "테스트",
                "VOUCHER",
                "상품-" + id,
                brandName,
                null,
                null,
                null,
                300,
                400,
                Instant.parse("2026-07-23T00:00:00Z")
        );
        ReflectionTestUtils.setField(product, "gifticonProductId", id);
        return product;
    }

    private GifticonTemplateSnapshot template() {
        return new GifticonTemplateSnapshot(
                2026072304113962800L,
                "편의점-츄파츕스",
                LocalDateTime.of(2026, 7, 23, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "ALIVE",
                "UNLIMITED",
                0L,
                "만나봄",
                "https://example.com/card.jpg",
                "테스트",
                "VOUCHER",
                "퍼페티)츄파춥스",
                "GS25",
                "https://example.com/product.png",
                "https://example.com/thumb.png",
                "https://example.com/brand.png",
                300
        );
    }
}
