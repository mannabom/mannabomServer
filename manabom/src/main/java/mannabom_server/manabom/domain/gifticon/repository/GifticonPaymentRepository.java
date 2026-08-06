package mannabom_server.manabom.domain.gifticon.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GifticonPaymentRepository extends JpaRepository<GifticonPayment, Long> {

    @EntityGraph(attributePaths = {"messageRequest", "product"})
    @Query("select p from GifticonPayment p where p.gifticonPaymentId = :paymentId")
    Optional<GifticonPayment> findByIdWithMessageRequest(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GifticonPayment p where p.gifticonPaymentId = :paymentId")
    Optional<GifticonPayment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GifticonPayment p where p.orderId = :orderId")
    Optional<GifticonPayment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GifticonPayment p where p.messageRequest.id = :messageRequestId")
    Optional<GifticonPayment> findByMessageRequestIdForUpdate(
            @Param("messageRequestId") Long messageRequestId
    );

    @Query("""
            select p.gifticonPaymentId
            from GifticonPayment p
            where p.status in :statuses
              and p.refundAttemptCount < :maxAttempts
              and (p.status <> :processingStatus
                   or p.lastRefundAttemptAt < :processingStaleBefore)
            order by p.createdAt asc
            """)
    List<Long> findRefundRetryIds(
            @Param("statuses") Collection<GifticonPaymentStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("processingStatus") GifticonPaymentStatus processingStatus,
            @Param("processingStaleBefore") Instant processingStaleBefore,
            Pageable pageable
    );

    @Query("""
            select p.gifticonPaymentId
            from GifticonPayment p
            where p.status = :paidStatus
              and p.messageRequest is null
              and p.messageCreationStatus in :messageStatuses
              and p.messageCreationAttemptCount < :maxAttempts
              and (p.messageCreationStatus <> :processingStatus
                   or p.lastMessageCreationAttemptAt < :processingStaleBefore)
            order by p.createdAt asc
            """)
    List<Long> findMessageCreationRetryIds(
            @Param("paidStatus") GifticonPaymentStatus paidStatus,
            @Param("messageStatuses") Collection<GifticonMessageCreationStatus> messageStatuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("processingStatus") GifticonMessageCreationStatus processingStatus,
            @Param("processingStaleBefore") Instant processingStaleBefore,
            Pageable pageable
    );

    @Query("""
            select p.gifticonPaymentId
            from GifticonPayment p
            where p.status = :status
              and p.messageRequest is null
              and p.approvedAt <= :approvedBefore
            order by p.approvedAt asc
            """)
    List<Long> findUnusedPaidPaymentIds(
            @Param("status") GifticonPaymentStatus status,
            @Param("approvedBefore") Instant approvedBefore,
            Pageable pageable
    );

    @Query("""
            select p
            from GifticonPayment p
            left join p.messageRequest messageRequest
            where (:paymentStatus is null or p.status = :paymentStatus)
              and (:messageCreationStatus is null
                   or p.messageCreationStatus = :messageCreationStatus)
              and (:messageRequestStatus is null
                   or messageRequest.status = :messageRequestStatus)
              and (:userId is null or p.userId = :userId)
              and (:orderId = '' or lower(p.orderId) like lower(concat('%', :orderId, '%')))
              and (
                    :attentionOnly = false
                    or p.status = :refundFailedStatus
                    or (p.status = :refundProcessingStatus
                        and (p.lastRefundAttemptAt is null
                             or p.lastRefundAttemptAt < :refundStaleBefore))
                    or p.refundAttemptCount >= :maxRefundAttempts
                    or (p.status = :paidStatus
                        and p.messageRequest is null
                        and p.approvedAt is not null
                        and p.approvedAt < :messageStaleBefore)
                    or (p.messageCreationStatus = :messageFailedStatus
                        and p.status = :paidStatus)
              )
            order by p.createdAt desc
            """)
    Page<GifticonPayment> findForAdmin(
            @Param("paymentStatus") GifticonPaymentStatus paymentStatus,
            @Param("messageCreationStatus") GifticonMessageCreationStatus messageCreationStatus,
            @Param("messageRequestStatus") mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus messageRequestStatus,
            @Param("userId") Long userId,
            @Param("orderId") String orderId,
            @Param("attentionOnly") boolean attentionOnly,
            @Param("refundFailedStatus") GifticonPaymentStatus refundFailedStatus,
            @Param("refundProcessingStatus") GifticonPaymentStatus refundProcessingStatus,
            @Param("paidStatus") GifticonPaymentStatus paidStatus,
            @Param("messageFailedStatus") GifticonMessageCreationStatus messageFailedStatus,
            @Param("refundStaleBefore") Instant refundStaleBefore,
            @Param("messageStaleBefore") Instant messageStaleBefore,
            @Param("maxRefundAttempts") int maxRefundAttempts,
            Pageable pageable
    );
}
