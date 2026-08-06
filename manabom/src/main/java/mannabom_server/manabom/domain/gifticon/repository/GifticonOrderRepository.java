package mannabom_server.manabom.domain.gifticon.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface GifticonOrderRepository extends JpaRepository<GifticonOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select gifticonOrder
            from GifticonOrder gifticonOrder
            where gifticonOrder.gifticonOrderId = :gifticonOrderId
            """)
    Optional<GifticonOrder> findByIdForUpdate(
            @Param("gifticonOrderId") Long gifticonOrderId
    );

    Optional<GifticonOrder> findByPayment_GifticonPaymentId(Long paymentId);

    @Query("""
            select gifticonOrder.gifticonOrderId
            from GifticonOrder gifticonOrder
            where gifticonOrder.status in :statuses
              and gifticonOrder.attemptCount < :maxAttempts
              and (gifticonOrder.status <> :processingStatus
                   or gifticonOrder.lastAttemptAt < :processingStaleBefore)
            order by gifticonOrder.createdAt asc
            """)
    List<Long> findRetryableOrderIds(
            @Param("statuses") Collection<GifticonOrderStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("processingStatus") GifticonOrderStatus processingStatus,
            @Param("processingStaleBefore") Instant processingStaleBefore,
            Pageable pageable
    );
}
