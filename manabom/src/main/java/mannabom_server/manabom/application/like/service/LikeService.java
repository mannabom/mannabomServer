package mannabom_server.manabom.application.like.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.like.dto.response.SendLikeResponseDto;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import mannabom_server.manabom.application.pushService.PushMessages;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRequestRepository likeRequestRepository;
    private final TingWalletRepository tingWalletRepository;
    private final PushService pushService;
    private final RuntimePolicyService runtimePolicyService;
    private final TingWalletService tingWalletService;
    private final ProfileRepository profileRepository;

    @Transactional
    public SendLikeResponseDto sendLike(Long fromUserId, Long toProfileId, LikeSource source){
        if(fromUserId == null) throw new IllegalArgumentException("요청자의 userId가 비어있습니다.");
        if(toProfileId == null) throw new IllegalArgumentException("targetProfileId가 비어있습니다.");

        Profile toProfile = profileRepository.findById(toProfileId)
                .orElseThrow(() -> new IllegalArgumentException("대상의 프로필을 찾을 수 없습니다."));
        Long toUserId = toProfile.getUser().getUserId();

        if(fromUserId.equals(toUserId)) throw new IllegalArgumentException("자기자신에게 보낼 수 없습니다.");
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(fromUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(fromUserId)));

        likeRequestRepository.findByFromUserIdAndToUserId(fromUserId, toUserId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("이미 요청을 보냈습니다.");
                });

        int vipLikeRemains = 0;
        int membershipLikeRemains = 0;
        int likeCost = p.getTing().getCost().getLike();

        boolean vip = tingWallet.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                today
        );
        if(vip){
            vipLikeRemains = tingWallet.checkVipFreeLikesRemaining(today);
        }
        boolean membership = tingWallet.isMembershipActive(now);
        if(membership){
            membershipLikeRemains = tingWallet.checkMembershipFreeLikesRemaining(now);
        }

        if(tingWallet.getEventTing() >= likeCost) {
            tingWallet.spendEventTing(likeCost);
        } else if (vipLikeRemains > 0){
            tingWallet.consumeVipFreeLike(today);
        } else if (membershipLikeRemains > 0) {
            tingWallet.consumeMembershipFreeLike(now);
        } else if (tingWallet.getTing() >= likeCost) {
            tingWallet.spendTing(likeCost);
        } else {
            throw new IllegalStateException("보유 재화가 부족합니다.(팅, 아밴트 팅, 맴버쉽, vip 혜택권 등)");
        }

        LikeRequest likeRequest = new LikeRequest(fromUserId, toUserId, source);
        likeRequestRepository.save(likeRequest);

        try {
            pushService.sendToUser(toUserId, PushMessages.likeReceived(fromUserId));
        } catch (Exception e) {
            log.warn("좋아요 푸시 전송 실패 fromUserId={} toUserId={}", fromUserId, toUserId, e);
        }

        CheckTingWalletResponseDto response = tingWalletService.checkTingWallet(fromUserId);

        return SendLikeResponseDto.builder()
                .freeLikeNum(response.getFreeLikeNum())
                .freeMessageNum(response.getFreeMessageNum())
                .eventTingNum(response.getEventTingNum())
                .tingNum(response.getTingNum())
                .freeProfileNum(response.getFreeProfileNum())
                .freeLoveViewNum(response.getFreeLoveViewNum())
                .additionalProfileNum(response.getAdditionalProfileNum())
                .build();
    }
}
