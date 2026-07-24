package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.event.GifticonOrderReadyEvent;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GifticonOrderService {

    private final GifticonOrderRepository gifticonOrderRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GifticonOrderStatus prepareOrder(MessageRequest messageRequest) {
        GifticonProduct product = messageRequest.getGifticonProduct();
        if (product == null) {
            throw new IllegalArgumentException("메시지 요청에 기프티콘 상품이 없습니다.");
        }
        if (!messageRequest.hasCapturedGiftPayment()) {
            throw new IllegalStateException("기프티콘 유상 팅 결제가 확정되지 않았습니다.");
        }
        if (!product.isOrderableAt(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("현재 발송할 수 없는 기프티콘 상품입니다.");
        }

        Profile senderProfile = profileRepository.findByUser_UserId(messageRequest.getFromUserId())
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 발신자의 프로필을 찾을 수 없습니다."));
        String senderNickname = senderProfile.getNickName();
        if (senderNickname == null || senderNickname.isBlank()) {
            throw new IllegalStateException("기프티콘을 보내려면 프로필 닉네임이 필요합니다.");
        }

        User receiver = userRepository.findById(messageRequest.getToUserId())
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 수신자를 찾을 수 없습니다."));
        String receiverPhone = normalizeKoreanMobile(receiver.getPhoneNum());
        String receiverName = receiver.getUserName();
        if (receiverName == null || receiverName.isBlank()) {
            receiverName = "만나봄 회원";
        }

        String externalOrderId = "MESSAGE-GIFT-" + messageRequest.getId();
        String externalKey = externalOrderId + "-" + messageRequest.getToUserId();
        GifticonOrder order = gifticonOrderRepository.save(new GifticonOrder(
                messageRequest,
                senderNickname,
                receiverPhone,
                receiverName,
                externalKey,
                externalOrderId
        ));
        eventPublisher.publishEvent(new GifticonOrderReadyEvent(order.getGifticonOrderId()));
        return order.getStatus();
    }

    private String normalizeKoreanMobile(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalStateException("기프티콘을 받으려면 휴대폰 번호 등록이 필요합니다.");
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("01[016789][0-9]{7,8}")) {
            throw new IllegalStateException("등록된 휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return digits;
    }
}
