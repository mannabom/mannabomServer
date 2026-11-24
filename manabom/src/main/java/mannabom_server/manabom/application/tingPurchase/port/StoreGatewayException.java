package mannabom_server.manabom.application.tingPurchase.port;

/** 스토어 통신 장애 등 재시도 가능한 실패. */
public class StoreGatewayException extends RuntimeException {
    public StoreGatewayException(String message) {
        super(message);
    }

    public StoreGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
