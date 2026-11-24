package mannabom_server.manabom.application.tingPurchase.port;

/** 스토어가 구매를 유효하지 않다고 판정한 경우(재시도해도 결과가 바뀌지 않는 실패). */
public class StorePurchaseInvalidException extends RuntimeException {
    public StorePurchaseInvalidException(String message) {
        super(message);
    }

    public StorePurchaseInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
