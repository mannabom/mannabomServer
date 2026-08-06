package mannabom_server.manabom.application.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonPaymentResponse;
import mannabom_server.manabom.application.admin.enums.GifticonPaymentAttentionReason;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.application.gifticon.service.GifticonPaymentService;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGifticonPaymentServiceTest {

    @Mock
    private GifticonPaymentRepository paymentRepository;
    @Mock
    private GifticonPaymentService paymentService;
    @Mock
    private GifticonPaymentGateway paymentGateway;
    @Mock
    private AdminAuditService adminAuditService;

    private AdminGifticonPaymentService service;

    @BeforeEach
    void setUp() {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.getRefundRetry().setMaxAttempts(10);
        properties.getUnusedPaymentRefund().setGracePeriodMinutes(30);
        service = new AdminGifticonPaymentService(
                paymentRepository,
                paymentService,
                paymentGateway,
                properties,
                adminAuditService,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void supportCanSeeApplicationAndTossStatusMismatch() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdWithMessageRequest(1L))
                .thenReturn(Optional.of(payment));
        when(paymentGateway.getPayment(new GifticonPaymentGateway.PaymentLookup(
                "payment-key",
                "GIFTICON_123456"
        ))).thenReturn(new GifticonPaymentGateway.PaymentResult(
                "payment-key",
                "GIFTICON_123456",
                11_100,
                "CANCELED"
        ));

        AdminGifticonPaymentResponse response = service.getPayment(
                principal(AdminRole.SUPPORT),
                1L
        );

        assertThat(response.tossStatusMismatch()).isTrue();
        assertThat(response.attentionReasons())
                .extracting(AdminGifticonPaymentResponse.AttentionReason::code)
                .contains(GifticonPaymentAttentionReason.TOSS_STATUS_MISMATCH.name());
    }

    @Test
    void refundFailureIsShownAsAttentionReason() {
        GifticonPayment payment = paidPayment();
        payment.requestRefund();
        payment.startRefund(Instant.now().minusSeconds(120));
        payment.markRefundFailed("토스 취소 실패");
        when(paymentRepository.findByIdWithMessageRequest(1L))
                .thenReturn(Optional.of(payment));
        when(paymentGateway.getPayment(new GifticonPaymentGateway.PaymentLookup(
                "payment-key",
                "GIFTICON_123456"
        ))).thenReturn(new GifticonPaymentGateway.PaymentResult(
                "payment-key",
                "GIFTICON_123456",
                11_100,
                "DONE"
        ));

        AdminGifticonPaymentResponse response = service.getPayment(
                principal(AdminRole.FINANCE),
                1L
        );

        assertThat(response.attentionReasons())
                .extracting(AdminGifticonPaymentResponse.AttentionReason::code)
                .contains(GifticonPaymentAttentionReason.REFUND_FAILED.name());
    }

    @Test
    void operatorCanRetryMessageAndActionIsAudited() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdWithMessageRequest(1L))
                .thenReturn(Optional.of(payment));
        when(paymentGateway.getPayment(new GifticonPaymentGateway.PaymentLookup(
                "payment-key",
                "GIFTICON_123456"
        ))).thenReturn(new GifticonPaymentGateway.PaymentResult(
                "payment-key",
                "GIFTICON_123456",
                11_100,
                "DONE"
        ));

        service.retryMessage(
                principal(AdminRole.OPERATOR),
                1L,
                "메시지 생성 장애 복구",
                "127.0.0.1"
        );

        verify(paymentService).retryMessageForAdmin(1L);
        verify(adminAuditService).log(
                eq(99L),
                eq(AdminAuditActionType.GIFTICON_PAYMENT_MESSAGE_RETRY),
                eq(AdminAuditTargetType.GIFTICON_PAYMENT),
                eq(1L),
                anyString(),
                anyString(),
                eq("메시지 생성 장애 복구"),
                eq("127.0.0.1")
        );
    }

    @Test
    void supportCannotRetryMessage() {
        assertThatThrownBy(() -> service.retryMessage(
                principal(AdminRole.SUPPORT),
                1L,
                "재시도",
                "127.0.0.1"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("권한");
    }

    @Test
    void failedForcedRefundStillLeavesAuditLogWithFailure() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdWithMessageRequest(1L))
                .thenReturn(Optional.of(payment));
        doThrow(new IllegalStateException("토스 환불 실패"))
                .when(paymentService).refundForAdmin(1L);

        assertThatThrownBy(() -> service.forceRefund(
                principal(AdminRole.FINANCE),
                1L,
                "고객 문의 확인 후 강제 환불",
                "127.0.0.1"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("토스 환불 실패");

        verify(adminAuditService).log(
                eq(99L),
                eq(AdminAuditActionType.GIFTICON_PAYMENT_FORCE_REFUND),
                eq(AdminAuditTargetType.GIFTICON_PAYMENT),
                eq(1L),
                anyString(),
                org.mockito.ArgumentMatchers.contains("토스 환불 실패"),
                eq("고객 문의 확인 후 강제 환불"),
                eq("127.0.0.1")
        );
    }

    @Test
    void adminActionRequiresReasonEvenWhenServiceIsCalledDirectly() {
        assertThatThrownBy(() -> service.forceRefund(
                principal(AdminRole.SUPER_ADMIN),
                1L,
                " ",
                "127.0.0.1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사유");
    }

    private GifticonPayment paidPayment() {
        GifticonProduct product = new GifticonProduct(100L);
        ReflectionTestUtils.setField(product, "gifticonProductId", 10L);
        ReflectionTestUtils.setField(product, "productName", "아메리카노");
        ReflectionTestUtils.setField(product, "salePrice", 11_100);
        GifticonPayment payment = new GifticonPayment(
                7L,
                product,
                "GIFTICON_123456",
                "CUSTOMER_123456",
                15L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
        ReflectionTestUtils.setField(payment, "gifticonPaymentId", 1L);
        Instant now = Instant.now();
        ReflectionTestUtils.setField(payment, "createdAt", now.minusSeconds(3600));
        ReflectionTestUtils.setField(payment, "updatedAt", now);
        payment.startConfirmation("payment-key", now, now.minusSeconds(60));
        payment.markPaid(now.minusSeconds(60));
        return payment;
    }

    private AdminPrincipal principal(AdminRole role) {
        return new AdminPrincipal(99L, "admin", Set.of(role));
    }
}
