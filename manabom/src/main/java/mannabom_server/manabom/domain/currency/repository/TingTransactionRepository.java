package mannabom_server.manabom.domain.currency.repository;

import mannabom_server.manabom.domain.currency.entity.TingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TingTransactionRepository extends JpaRepository<TingTransaction, Long> {
}
