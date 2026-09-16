package mannabom_server.manabom.domain.tingPurchase.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TingPurchaseRepository extends JpaRepository<TingPurchase, Long> {

    @EntityGraph(attributePaths = {"payment", "product"})
    @Query("select t from TingPurchase t where t.paymentId = :paymentId")
    Optional<TingPurchase> findByIdWithDetails(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TingPurchase t join fetch t.product where t.paymentId = :paymentId")
    Optional<TingPurchase> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @EntityGraph(attributePaths = {"payment", "product"})
    @Query("""
            select t from TingPurchase t
            where t.payment.userId = :userId
            order by t.payment.createdAt desc
            """)
    List<TingPurchase> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
