package mannabom_server.manabom.tests;

import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.infrastructure.config.QueryDslConfig;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(QueryDslConfig.class)
class TingWalletRepositoryTest {

    @Autowired
    TingWalletRepository tingWalletRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("findByUserIdForUpdate: 유저 지갑이 없으면 Optional.empty")
    void findByUserIdForUpdate_empty_when_not_exists() {
        assertTrue(tingWalletRepository.findByUserIdForUpdate(999L).isEmpty());
    }

    @Test
    @DisplayName("findByUserIdForUpdate: 존재하면 조회되고 PESSIMISTIC_WRITE 락 모드로 로드된다")
    void findByUserIdForUpdate_applies_pessimistic_write_lock() {
        long userId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
        tingWalletRepository.save(new TingWallet(userId));
        em.flush();
        em.clear();

        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(userId).orElseThrow();

        Session session = em.unwrap(Session.class);
        LockMode lockMode = session.getCurrentLockMode(wallet);

        assertEquals(LockMode.PESSIMISTIC_WRITE, lockMode);
        assertEquals(userId, wallet.getUserId());
    }
}
