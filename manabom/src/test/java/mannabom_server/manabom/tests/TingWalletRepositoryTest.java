package mannabom_server.manabom.tests;

import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
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
        tingWalletRepository.save(new TingWallet(1L));
        em.flush();
        em.clear();

        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(1L).orElseThrow();

        Session session = em.unwrap(Session.class);
        LockMode lockMode = session.getCurrentLockMode(wallet);

        assertEquals(LockMode.PESSIMISTIC_WRITE, lockMode);
        assertEquals(1L, wallet.getUserId());
    }
}
