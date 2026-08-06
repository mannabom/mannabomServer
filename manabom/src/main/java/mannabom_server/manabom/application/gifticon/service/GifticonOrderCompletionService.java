package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.response.ChatGifticonInfo;
import mannabom_server.manabom.application.gifticon.event.ChatGifticonMessageCreatedEvent;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GifticonOrderCompletionService {

    private final GifticonOrderRepository orderRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRequested(Long orderId, Instant requestedAt) {
        GifticonOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 주문을 찾을 수 없습니다."));
        if (order.getStatus() == GifticonOrderStatus.REQUESTED) {
            return;
        }
        if (order.getStatus() != GifticonOrderStatus.PROCESSING) {
            throw new IllegalStateException("발송 처리 중인 기프티콘 주문이 아닙니다.");
        }

        order.markRequested(requestedAt);
        GifticonPayment payment = order.getPayment();
        if (payment.getPurpose() != GifticonPaymentPurpose.CHAT
                || payment.getChatMessage() != null) {
            return;
        }

        User sender = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 발신자를 찾을 수 없습니다."));
        Profile senderProfile = profileRepository.findByUser(sender)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 발신자 프로필을 찾을 수 없습니다."));
        String content = payment.getProduct().getProductName() + " 기프티콘을 보냈습니다.";
        ChatMessage chatMessage = chatMessageRepository.save(ChatMessage.builder()
                .room(payment.getChatRoom())
                .type(ChatMessageType.GIFTICON)
                .content(content)
                .user(sender)
                .build());
        payment.attachToChatMessage(chatMessage);

        eventPublisher.publishEvent(new ChatGifticonMessageCreatedEvent(
                payment.getChatRoom().getId(),
                chatMessage.getId(),
                payment.getUserId(),
                payment.getReceiverUserId(),
                senderProfile.getNickName(),
                content,
                chatMessage.getCreatedAt(),
                ChatGifticonInfo.from(payment)
        ));
    }
}
