package mannabom_server.manabom.domain.currency.repository;

import mannabom_server.manabom.domain.currency.entity.TingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TingTransactionRepository extends JpaRepository<TingTransaction, Long> {
    Optional<TingTransaction> findByIdempotencyKey(String idempotencyKey);
}
