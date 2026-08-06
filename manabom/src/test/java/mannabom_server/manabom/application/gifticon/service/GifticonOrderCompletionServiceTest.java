package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.application.gifticon.event.ChatGifticonMessageCreatedEvent;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentPurpose;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GifticonOrderCompletionServiceTest {

    @Mock
    private GifticonOrderRepository orderRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GifticonOrderCompletionService service;

    @BeforeEach
    void setUp() {
        service = new GifticonOrderCompletionService(
                orderRepository,
                chatMessageRepository,
                userRepository,
                profileRepository,
                eventPublisher
        );
    }

    @Test
    void revealsChatGifticonOnlyAfterExternalRequestWasAccepted() {
        GifticonOrder order = mock(GifticonOrder.class);
        GifticonPayment payment = mock(GifticonPayment.class);
        GifticonProduct product = mock(GifticonProduct.class);
        ChatRoom room = mock(ChatRoom.class);
        User sender = mock(User.class);
        Profile senderProfile = mock(Profile.class);
        Instant createdAt = Instant.parse("2026-08-06T12:00:00Z");

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(order.getStatus()).thenReturn(GifticonOrderStatus.PROCESSING);
        when(order.getPayment()).thenReturn(payment);
        when(payment.getPurpose()).thenReturn(GifticonPaymentPurpose.CHAT);
        when(payment.getProduct()).thenReturn(product);
        when(payment.getChatRoom()).thenReturn(room);
        when(payment.getUserId()).thenReturn(1L);
        when(payment.getReceiverUserId()).thenReturn(2L);
        when(payment.getGifticonPaymentId()).thenReturn(20L);
        when(room.getId()).thenReturn(30L);
        when(product.getGifticonProductId()).thenReturn(40L);
        when(product.getProductName()).thenReturn("아메리카노");
        when(product.getBrandName()).thenReturn("카페");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(profileRepository.findByUser(sender)).thenReturn(Optional.of(senderProfile));
        when(senderProfile.getNickName()).thenReturn("봄이");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 50L);
            ReflectionTestUtils.setField(message, "createdAt", createdAt);
            return message;
        });

        service.completeRequested(10L, createdAt);

        verify(order).markRequested(createdAt);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent())
                .isEqualTo("아메리카노 기프티콘을 보냈습니다.");
        verify(payment).attachToChatMessage(messageCaptor.getValue());
        verify(eventPublisher).publishEvent(any(ChatGifticonMessageCreatedEvent.class));
    }
}
