package mannabom_server.manabom.application.currency.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.currency.entity.TingTransaction;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingBalanceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TingTransactionRecorder {

    private final TingTransactionRepository tingTransactionRepository;

    public TingTransaction recordPaid(
            TingWallet wallet,
            TingTransactionType transactionType,
            int amountDelta,
            TingTransactionReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        return record(
                wallet,
                TingBalanceType.PAID,
                transactionType,
                amountDelta,
                wallet.getTing(),
                referenceType,
                referenceId,
                idempotencyKey,
                description
        );
    }

    public TingTransaction recordEvent(
            TingWallet wallet,
            TingTransactionType transactionType,
            int amountDelta,
            TingTransactionReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        return record(
                wallet,
                TingBalanceType.EVENT,
                transactionType,
                amountDelta,
                wallet.getEventTing(),
                referenceType,
                referenceId,
                idempotencyKey,
                description
        );
    }

    private TingTransaction record(
            TingWallet wallet,
            TingBalanceType balanceType,
            TingTransactionType transactionType,
            int amountDelta,
            int balanceAfter,
            TingTransactionReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        if (wallet == null) {
            throw new IllegalArgumentException("팅 거래를 기록할 지갑은 필수입니다.");
        }
        if (StringUtils.hasText(idempotencyKey)) {
            TingTransaction existing = tingTransactionRepository
                    .findByIdempotencyKey(idempotencyKey.trim())
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        return tingTransactionRepository.save(new TingTransaction(
                wallet.getUserId(),
                balanceType,
                transactionType,
                amountDelta,
                balanceAfter,
                referenceType,
                referenceId,
                idempotencyKey,
                description
        ));
    }
}
