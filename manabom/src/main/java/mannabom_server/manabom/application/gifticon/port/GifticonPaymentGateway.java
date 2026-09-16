package mannabom_server.manabom.application.gifticon.port;

public interface GifticonPaymentGateway {

    PaymentResult confirm(ConfirmPaymentCommand command);

    PaymentResult cancel(CancelPaymentCommand command);

    PaymentResult getPayment(PaymentLookup query);

    record ConfirmPaymentCommand(
            String paymentKey,
            String orderId,
            int amount,
            String idempotencyKey
    ) {
    }

    record CancelPaymentCommand(
            String paymentKey,
            String cancelReason,
            String idempotencyKey
    ) {
    }

    record PaymentLookup(
            String paymentKey,
            String orderId
    ) {
    }

    record PaymentResult(
            String paymentKey,
            String orderId,
            int totalAmount,
            String status
    ) {
    }
}
