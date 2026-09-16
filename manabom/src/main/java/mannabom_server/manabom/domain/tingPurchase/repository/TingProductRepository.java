package mannabom_server.manabom.domain.tingPurchase.repository;

import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TingProductRepository extends JpaRepository<TingProduct, Long> {

    Optional<TingProduct> findByProductCode(String productCode);

    List<TingProduct> findAllByActiveTrueOrderBySortOrderAscTingProductIdAsc();

    List<TingProduct> findAllByOrderBySortOrderAscTingProductIdAsc();
}
