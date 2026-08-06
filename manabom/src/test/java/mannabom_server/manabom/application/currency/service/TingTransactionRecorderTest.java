package mannabom_server.manabom.application.currency.service;

import mannabom_server.manabom.domain.currency.entity.TingTransaction;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TingTransactionRecorderTest {

    @Mock
    private TingTransactionRepository repository;

    @Test
    void returnsExistingTransactionForSameIdempotencyKey() {
        TingWallet wallet = new TingWallet(1L);
        wallet.addEventTing(10);
        TingTransaction existing = new TingTransaction(
                1L,
                mannabom_server.manabom.domain.currency.enums.TingBalanceType.EVENT,
                TingTransactionType.SIGNUP_BONUS,
                10,
                10,
                TingTransactionReferenceType.USER,
                "1",
                "SIGNUP:1:BONUS",
                "보너스"
        );
        when(repository.findByIdempotencyKey("SIGNUP:1:BONUS"))
                .thenReturn(Optional.of(existing));

        TingTransaction result = new TingTransactionRecorder(repository).recordEvent(
                wallet,
                TingTransactionType.SIGNUP_BONUS,
                10,
                TingTransactionReferenceType.USER,
                "1",
                "SIGNUP:1:BONUS",
                "보너스"
        );

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
