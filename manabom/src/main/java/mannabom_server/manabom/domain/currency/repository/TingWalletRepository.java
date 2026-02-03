package mannabom_server.manabom.domain.currency.repository;

import mannabom_server.manabom.domain.currency.entity.TingWallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TingWalletRepository extends JpaRepository<TingWallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from TingWallet w where w.userId = :userId")
    Optional<TingWallet> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select w from TingWallet w where w.userId = :userId")
    Optional<TingWallet> findByUserId(@Param("userId") Long userId);
}
