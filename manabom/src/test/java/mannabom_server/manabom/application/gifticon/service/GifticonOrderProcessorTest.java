package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.application.gifticon.port.GifticonOrderRequester;
import mannabom_server.manabom.application.gifticon.port.GifticonTokenCipher;
import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;
import mannabom_server.manabom.domain.gifticon.entity.GifticonOrder;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonOrderStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonOrderRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GifticonOrderProcessorTest {

    @Mock
    private GifticonOrderRepository gifticonOrderRepository;

    @Mock
    private GifticonOrderRequester gifticonOrderRequester;

    @Mock
    private GifticonTokenCipher gifticonTokenCipher;

    private GifticonOrderProcessor processor;

    @BeforeEach
    void setUp() {
        GiftbizProperties properties = new GiftbizProperties();
        properties.getOrder().getRetry().setMaxAttempts(5);
        processor = new GifticonOrderProcessor(
                gifticonOrderRepository,
                gifticonOrderRequester,
                gifticonTokenCipher,
                properties
        );
    }

    @Test
    void requestsGiftWithDecryptedTokenAndMarksRequested() {
        GifticonOrder order = order();
        when(gifticonOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(gifticonTokenCipher.decrypt("encrypted-token")).thenReturn("plain-token");

        processor.process(1L);

        ArgumentCaptor<GifticonOrderCommand> commandCaptor =
                ArgumentCaptor.forClass(GifticonOrderCommand.class);
        verify(gifticonOrderRequester).requestGift(commandCaptor.capture());
        assertThat(commandCaptor.getValue().templateToken()).isEqualTo("plain-token");
        assertThat(order.getStatus()).isEqualTo(GifticonOrderStatus.REQUESTED);
        assertThat(order.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void marksFailedWhenKakaoRequestFails() {
        GifticonOrder order = order();
        when(gifticonOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(gifticonTokenCipher.decrypt("encrypted-token")).thenReturn("plain-token");
        doThrow(new IllegalStateException("temporary failure"))
                .when(gifticonOrderRequester)
                .requestGift(org.mockito.ArgumentMatchers.any());

        processor.process(1L);

        assertThat(order.getStatus()).isEqualTo(GifticonOrderStatus.FAILED);
        assertThat(order.getAttemptCount()).isEqualTo(1);
        assertThat(order.getFailureReason()).contains("temporary failure");
    }

    private GifticonOrder order() {
        GifticonProduct product = new GifticonProduct(100L);
        product.configureEncryptedTemplateToken("encrypted-token");
        MessageRequest messageRequest = new MessageRequest(
                1L,
                2L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH,
                product
        );
        return new GifticonOrder(
                messageRequest,
                "encrypted-token",
                "01012345678",
                "수신자",
                "MESSAGE-GIFT-1-2",
                "MESSAGE-GIFT-1"
        );
    }
}
