package mannabom_server.manabom.application.tingPurchase.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingProductCatalogResponse;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingProductResponse;
import mannabom_server.manabom.domain.tingPurchase.repository.TingProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TingProductService {

    private final TingProductRepository productRepository;

    @Transactional(readOnly = true)
    public TingProductCatalogResponse getCatalog(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 정보가 필요합니다.");
        }
        return new TingProductCatalogResponse(
                PurchaseAccountToken.forUser(userId),
                productRepository.findAllByActiveTrueOrderBySortOrderAscTingProductIdAsc()
                        .stream()
                        .map(TingProductResponse::from)
                        .toList()
        );
    }
}
