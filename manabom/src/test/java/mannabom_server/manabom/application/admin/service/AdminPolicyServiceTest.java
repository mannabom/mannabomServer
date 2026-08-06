package mannabom_server.manabom.application.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdatePolicyRequest;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.domain.gifticon.service.GifticonPriceCalculator;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPolicyServiceTest {

    @Test
    void changingGifticonMarkupRecalculatesEveryProduct() {
        RuntimePolicyService runtimePolicyService = mock(RuntimePolicyService.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        GifticonProductRepository productRepository = mock(GifticonProductRepository.class);
        GifticonPriceCalculator calculator = new GifticonPriceCalculator(BigDecimal.TEN, 100);
        AdminPolicyService service = new AdminPolicyService(
                runtimePolicyService,
                adminAuditService,
                new ObjectMapper(),
                productRepository,
                calculator
        );
        RuntimePolicySnapshot before = policy(new BigDecimal("10"));
        RuntimePolicySnapshot after = policy(new BigDecimal("20"));
        when(runtimePolicyService.snapshot()).thenReturn(before, after);
        GifticonProduct first = product(1L, 1_000);
        GifticonProduct second = product(2L, 1_550);
        when(productRepository.findAll()).thenReturn(List.of(first, second));

        AdminUpdatePolicyRequest request = new AdminUpdatePolicyRequest();
        ReflectionTestUtils.setField(request, "key", "gifticon.pricing.markupPercent");
        ReflectionTestUtils.setField(request, "value", new BigDecimal("20"));
        ReflectionTestUtils.setField(request, "reason", "가격 정책 변경");

        service.updatePolicy(
                new AdminPrincipal(1L, "admin", Set.of(AdminRole.SUPER_ADMIN)),
                request,
                "127.0.0.1"
        );

        verify(runtimePolicyService).updateGifticonMarkupPercent(new BigDecimal("20"));
        assertThat(first.getSalePrice()).isEqualTo(1_200);
        assertThat(second.getSalePrice()).isEqualTo(1_900);
    }

    private RuntimePolicySnapshot policy(BigDecimal markupPercent) {
        return RuntimePolicySnapshot.builder()
                .gifticon(RuntimePolicySnapshot.Gifticon.builder()
                        .pricing(RuntimePolicySnapshot.Gifticon.Pricing.builder()
                                .markupPercent(markupPercent)
                                .roundUnit(100)
                                .build())
                        .build())
                .build();
    }

    private GifticonProduct product(long id, int productPrice) {
        GifticonProduct product = new GifticonProduct(id);
        product.synchronize(
                "템플릿-" + id,
                null,
                null,
                "ALIVE",
                "UNLIMITED",
                0L,
                "만나봄",
                null,
                null,
                "VOUCHER",
                "상품-" + id,
                "브랜드",
                null,
                null,
                null,
                productPrice,
                productPrice,
                Instant.parse("2026-08-06T00:00:00Z")
        );
        return product;
    }
}
