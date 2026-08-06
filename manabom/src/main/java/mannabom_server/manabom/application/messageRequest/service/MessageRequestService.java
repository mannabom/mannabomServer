package mannabom_server.manabom.application.messageRequest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.gifticon.service.GifticonOrderService;
import mannabom_server.manabom.application.gifticon.event.GifticonPaymentRefundRequestedEvent;
import mannabom_server.manabom.application.messageRequest.dto.response.SendMessageResponseDto;
import mannabom_server.manabom.application.pushService.PushMessages;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.application.signal.dto.response.RespondSignalResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionReferenceType;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.matching.repository.LoveViewRecommendHistoryRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRequestService {
    private final MessageRequestRepository messageRequestRepository;
    private final PushService pushService;
    private final ProfileRepository profileRepository;
    private final TingWalletRepository tingWalletRepository;
    private final RuntimePolicyService runtimePolicyService;
    private final TingWalletService tingWalletService;
    private final ProfileRecommendHistoryRepository profileRecommendHistoryRepository;
    private final LoveViewRecommendHistoryRepository loveViewRecommendHistoryRepository;
    private final ChatRoomService chatRoomService;
    private final GifticonPaymentRepository gifticonPaymentRepository;
    private final GifticonOrderService gifticonOrderService;
    private final TingTransactionRecorder tingTransactionRecorder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void validateMessageIntent(
            Long fromUserId,
            Long toProfileId,
            String message,
            MessageSource source
    ) {
        Long toUserId = validateMessageRequestInput(
                fromUserId,
                toProfileId,
                message,
                source
        );
        ensureMessageRequestDoesNotExist(fromUserId, toUserId);
    }

    @Transactional
    public Long sendPaidGifticonMessage(Long gifticonPaymentId) {
        GifticonPayment payment = gifticonPaymentRepository
                .findByIdForUpdate(gifticonPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
        if (payment.getMessageRequest() != null) {
            return payment.getMessageRequest().getId();
        }
        if (payment.getStatus() != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("결제가 완료되지 않은 기프티콘입니다.");
        }

        createMessageRequest(
                payment.getUserId(),
                payment.getTargetProfileId(),
                payment.getMessage(),
                payment.getMessageSource(),
                payment.getGifticonPaymentId()
        );
        return payment.getMessageRequest().getId();
    }

    @Transactional
    public SendMessageResponseDto sendMessageRequest(
            Long fromUserId,
            Long toProfileId,
            String message,
            MessageSource source
    ) {
        return createMessageRequest(fromUserId, toProfileId, message, source, null);
    }

    private SendMessageResponseDto createMessageRequest(
            Long fromUserId,
            Long toProfileId,
            String message,
            MessageSource source,
            Long gifticonPaymentId
    ) {
        Long toUserId = validateMessageRequestInput(
                fromUserId,
                toProfileId,
                message,
                source
        );
        GifticonPayment gifticonPayment = findPaidGifticonPayment(
                fromUserId,
                gifticonPaymentId
        );
        GifticonProduct gifticonProduct = gifticonPayment == null
                ? null
                : gifticonPayment.getProduct();
        ensureMessageRequestDoesNotExist(fromUserId, toUserId);
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(fromUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(fromUserId)));

        MessageRequest messageRequest = messageRequestRepository.save(new MessageRequest(
                fromUserId,
                toUserId,
                message,
                source,
                gifticonProduct
        ));
        if (gifticonPayment != null) {
            gifticonPayment.attachTo(messageRequest);
        }

        int vipMessageRemains = 0;
        int membershipMessageRemains = 0;
        int messageCost = p.getTing().getCost().getMessage();

        boolean vip = tingWallet.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                today
        );
        if(vip){
            vipMessageRemains = tingWallet.checkVipFreeMessagesRemaining(today);
        }
        boolean membership = tingWallet.isMembershipActive(now);
        if(membership){
            membershipMessageRemains = tingWallet.checkMembershipFreeMessagesRemaining(now);
        }

        if(tingWallet.getEventTing() >= messageCost) {
            tingWallet.spendEventTing(messageCost);
            tingTransactionRecorder.recordEvent(
                    tingWallet,
                    TingTransactionType.MESSAGE_REQUEST,
                    -messageCost,
                    TingTransactionReferenceType.MESSAGE_REQUEST,
                    String.valueOf(messageRequest.getId()),
                    "MESSAGE_REQUEST:" + messageRequest.getId() + ":EVENT_COST",
                    "메시지 요청 비용"
            );
        } else if (vipMessageRemains > 0){
            tingWallet.consumeVipFreeMessage(today);
        } else if (membershipMessageRemains > 0) {
            tingWallet.consumeMembershipFreeMessage(now);
        } else if (tingWallet.getTing() >= messageCost) {
            tingWallet.spendTing(messageCost);
            tingTransactionRecorder.recordPaid(
                    tingWallet,
                    TingTransactionType.MESSAGE_REQUEST,
                    -messageCost,
                    TingTransactionReferenceType.MESSAGE_REQUEST,
                    String.valueOf(messageRequest.getId()),
                    "MESSAGE_REQUEST:" + messageRequest.getId() + ":PAID_COST",
                    "메시지 요청 비용"
            );
        } else {
            throw new IllegalStateException("보유 재화가 부족합니다.(팅, 아밴트 팅, 맴버쉽, vip 혜택권 등)");
        }

        try {
            pushService.sendToUser(toUserId, PushMessages.messageRequestReceived(fromUserId, message));
        } catch (Exception e) {
            log.warn("메시지 요청 푸시 전송 실패 fromUserId={} toUserId={}", fromUserId, toUserId, e);
        }

        CheckTingWalletResponseDto response = tingWalletService.checkTingWallet(fromUserId);

        return SendMessageResponseDto.builder()
                .freeLikeNum(response.getFreeLikeNum())
                .freeMessageNum(response.getFreeMessageNum())
                .eventTingNum(response.getEventTingNum())
                .tingNum(response.getTingNum())
                .freeProfileNum(response.getFreeProfileNum())
                .freeLoveViewNum(response.getFreeLoveViewNum())
                .additionalProfileNum(response.getAdditionalProfileNum())
                .build();

    }

    private GifticonPayment findPaidGifticonPayment(
            Long userId,
            Long gifticonPaymentId
    ) {
        if (gifticonPaymentId == null) {
            return null;
        }
        GifticonPayment payment = gifticonPaymentRepository
                .findByIdForUpdate(gifticonPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 결제를 찾을 수 없습니다."));
        if (!userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("본인의 기프티콘 결제만 사용할 수 있습니다.");
        }
        if (payment.getStatus() != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("결제가 완료되지 않은 기프티콘입니다.");
        }
        if (payment.getMessageRequest() != null) {
            throw new IllegalStateException("이미 메시지 요청에 사용된 기프티콘 결제입니다.");
        }
        if (!payment.getProduct().isOrderableAt(LocalDateTime.now())) {
            throw new IllegalStateException("현재 선택할 수 없는 기프티콘 상품입니다.");
        }
        return payment;
    }

    private Long validateMessageRequestInput(
            Long fromUserId,
            Long toProfileId,
            String message,
            MessageSource source
    ) {
        if (fromUserId == null) {
            throw new IllegalArgumentException("요청자의 정보를 찾을 수 없습니다.");
        }
        if (toProfileId == null) {
            throw new IllegalArgumentException("toProfileId가 비어있습니다.");
        }
        if (source == null) {
            throw new IllegalArgumentException("source가 비어있습니다.");
        }
        if (message != null && message.length() > 200) {
            throw new IllegalArgumentException("메시지는 200자를 초과할 수 없습니다.");
        }

        Profile toProfile = profileRepository.findById(toProfileId)
                .orElseThrow(() -> new IllegalArgumentException("대상자의 프로필을 찾을 수 없습니다."));
        Long toUserId = toProfile.getUser().getUserId();
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("본인에게 메시지 요청을 보낼 수 없습니다.");
        }
        return toUserId;
    }

    private void ensureMessageRequestDoesNotExist(Long fromUserId, Long toUserId) {
        messageRequestRepository.findByFromUserIdAndToUserId(fromUserId, toUserId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("이미 요청을 보냈습니다.");
                });
    }

    @Transactional
    public RespondSignalResponseDto respondMessageRequest(Long responderUserId, Long messageRequestId, boolean accepted, String rejectReason) {
        if (responderUserId == null) throw new IllegalArgumentException("응답자의 정보를 찾을 수 없습니다.");
        if (messageRequestId == null) throw new IllegalArgumentException("messageRequestId가 비어있습니다.");

        MessageRequest messageRequest = messageRequestRepository.findByIdForUpdate(messageRequestId)
                .orElseThrow(() -> new IllegalArgumentException("메시지 요청을 찾을 수 없습니다."));
        if (!responderUserId.equals(messageRequest.getToUserId())) {
            throw new IllegalArgumentException("메시지 요청을 받은 사용자만 응답할 수 있습니다.");
        }

        Long chatRoomId = null;
        GifticonOrderStatus gifticonOrderStatus = null;
        String gifticonPaymentStatus = null;
        if (accepted) {
            messageRequest.accept();
            chatRoomId = createChatRoom(messageRequest.getFromUserId(), messageRequest.getToUserId(), messageRequest.getSource());
            if (messageRequest.getGifticonProduct() != null) {
                gifticonPaymentStatus = confirmGiftPaymentForAcceptance(messageRequest);
                gifticonOrderStatus = gifticonOrderService.prepareOrder(messageRequest);
            }
        } else {
            messageRequest.reject(rejectReason);
            gifticonPaymentStatus = requestGifticonRefund(messageRequest);
        }

        try {
            pushService.sendToUser(messageRequest.getFromUserId(), PushMessages.messageRequestResponded(accepted, messageRequest.getId()));
        } catch (Exception e) {
            log.warn("메시지 요청 응답 푸시 전송 실패 messageRequestId={} accepted={}", messageRequestId, accepted, e);
        }

        return RespondSignalResponseDto.builder()
                .accepted(accepted)
                .chatRoomId(chatRoomId)
                .status(messageRequest.getStatus().name())
                .gifticonOrderStatus(gifticonOrderStatus == null ? null : gifticonOrderStatus.name())
                .gifticonPaymentStatus(
                        gifticonPaymentStatus
                )
                .build();
    }

    private String confirmGiftPaymentForAcceptance(MessageRequest messageRequest) {
        GifticonPayment payment = gifticonPaymentRepository
                .findByMessageRequestIdForUpdate(messageRequest.getId())
                .orElseThrow(() -> new IllegalStateException("기프티콘 결제 정보를 찾을 수 없습니다."));
        if (payment.getStatus() != GifticonPaymentStatus.PAID) {
            throw new IllegalStateException("기프티콘 원화 결제가 완료되지 않았습니다.");
        }
        return payment.getStatus().name();
    }

    private String requestGifticonRefund(MessageRequest messageRequest) {
        if (messageRequest.getGifticonProduct() == null) {
            return null;
        }
        GifticonPayment payment = gifticonPaymentRepository
                .findByMessageRequestIdForUpdate(messageRequest.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "메시지 요청의 기프티콘 결제를 찾을 수 없습니다."
                ));
        payment.requestRefund();
        eventPublisher.publishEvent(
                new GifticonPaymentRefundRequestedEvent(payment.getGifticonPaymentId())
        );
        return payment.getStatus().name();
    }

    private Long createChatRoom(Long requesterUserId, Long targetUserId, MessageSource source) {
        if (source == MessageSource.PROFILE_MATCH) {
            ProfileRecommendHistory history = profileRecommendHistoryRepository
                    .findTopByRequesterUserIdAndTargetUserIdOrderByRecommendedAtDesc(requesterUserId, targetUserId)
                    .orElseThrow(() -> new IllegalStateException("프로필 추천 이력이 없어 채팅방을 생성할 수 없습니다."));
            return chatRoomService.createProfileChatRoom(history);
        }

        LoveViewRecommendHistory history = loveViewRecommendHistoryRepository
                .findTopByRequesterUserIdAndTargetUserIdOrderByRecommendedAtDesc(requesterUserId, targetUserId)
                .orElseThrow(() -> new IllegalStateException("연애관 추천 이력이 없어 채팅방을 생성할 수 없습니다."));
        return chatRoomService.createLoveViewChatRoom(history);
    }
}
