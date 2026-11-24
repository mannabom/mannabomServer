package mannabom_server.manabom.domain.payment.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StorePaymentRepository extends JpaRepository<StorePayment, Long> {

    @EntityGraph(attributePaths = "payment")
    @Query("select s from StorePayment s where s.paymentId = :paymentId")
    Optional<StorePayment> findByIdWithPayment(@Param("paymentId") Long paymentId);

    /** payment 행까지 함께 잠근다(join fetch + PESSIMISTIC_WRITE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StorePayment s join fetch s.payment where s.paymentId = :paymentId")
    Optional<StorePayment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from StorePayment s join fetch s.payment
            where s.store = :store and s.purchaseKey = :purchaseKey
            """)
    Optional<StorePayment> findByStoreAndPurchaseKeyForUpdate(
            @Param("store") PurchaseStore store,
            @Param("purchaseKey") String purchaseKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from StorePayment s join fetch s.payment
            where s.store = :store and s.transactionId = :transactionId
            """)
    Optional<StorePayment> findByStoreAndTransactionIdForUpdate(
            @Param("store") PurchaseStore store,
            @Param("transactionId") String transactionId
    );

    @Query("""
            select s.paymentId
            from StorePayment s
            where s.payment.status = :paidStatus
              and s.consumeStatus in :consumeStatuses
              and s.consumeAttemptCount < :maxAttempts
              and (s.consumeStatus <> :processingStatus
                   or s.lastConsumeAttemptAt < :processingStaleBefore)
            order by s.payment.paidAt asc
            """)
    List<Long> findConsumeRetryIds(
            @Param("paidStatus") PaymentStatus paidStatus,
            @Param("consumeStatuses") Collection<StoreConsumeStatus> consumeStatuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("processingStatus") StoreConsumeStatus processingStatus,
            @Param("processingStaleBefore") Instant processingStaleBefore,
            Pageable pageable
    );
}
