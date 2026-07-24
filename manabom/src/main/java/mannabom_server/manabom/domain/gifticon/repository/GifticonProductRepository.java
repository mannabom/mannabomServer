package mannabom_server.manabom.domain.gifticon.repository;

import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface GifticonProductRepository extends JpaRepository<GifticonProduct, Long> {

    List<GifticonProduct> findAllByTemplateTraceIdIn(Collection<Long> templateTraceIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GifticonProduct product
               set product.available = false
             where product.available = true
               and product.lastSyncedAt < :syncedAt
            """)
    int markProductsNotSeenSinceUnavailable(@Param("syncedAt") Instant syncedAt);

    @Query("""
            select product
            from GifticonProduct product
            where product.available = true
              and product.templateToken is not null
              and (product.startAt is null or product.startAt <= :now)
              and (product.endAt is null or product.endAt > :now)
              and product.gifticonProductId > :cursor
              and (:category is null or product.brandName = :category)
            order by product.gifticonProductId asc
            """)
    List<GifticonProduct> findAvailableProductsAfter(
            @Param("now") LocalDateTime now,
            @Param("category") String category,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select product
            from GifticonProduct product
            where product.gifticonProductId > :cursor
            order by product.gifticonProductId asc
            """)
    List<GifticonProduct> findProductsForAdminAfter(
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
