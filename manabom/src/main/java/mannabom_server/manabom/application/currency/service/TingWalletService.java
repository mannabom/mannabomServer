package mannabom_server.manabom.application.currency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TingWalletService {
    private final TingWalletRepository tingWalletRepository;
    private final RuntimePolicyService runtimePolicyService;

    @Transactional
    public CheckTingWalletResponseDto checkTingWallet(Long userId) {
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        TingWallet w = tingWalletRepository.findByUserId(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int vipLikes = 0, vipMessages = 0;
        int memLikes = 0, memMessages = 0;
        int freeProfileNum = w.checkDailyProfile(today, p.getBenefit().getBasic().getDailyProfile());
        int freeLoveViewNum = w.checkDailyLoveView(today, p.getBenefit().getBasic().getDailyLoveView());
        int additionalProfileNum = 0;

        boolean vip = w.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                today
        );
        if (vip) {
            vipMessages = w.checkVipFreeMessagesRemaining(today);
            vipLikes = w.checkVipFreeLikesRemaining(today);
            additionalProfileNum += w.checkVipExtraProfilesRemaining(today);
        }

        boolean membership = w.isMembershipActive(now);
        if (membership) {
            memMessages = w.checkMembershipFreeMessagesRemaining(now);
            memLikes = w.checkMembershipFreeLikesRemaining(now);
            additionalProfileNum += w.checkMembershipExtraProfilesRemaining(now);
        }
        additionalProfileNum += w.checkExtraProfileByTing();

        int totalLikes = memLikes + vipLikes;
        int totalMessages = memMessages + vipMessages;
        int eventTingNum = w.getEventTing();
        int tingNum = w.getTing();

        return new CheckTingWalletResponseDto(totalLikes, totalMessages, eventTingNum, tingNum, freeProfileNum, freeLoveViewNum, additionalProfileNum);
    }
}
