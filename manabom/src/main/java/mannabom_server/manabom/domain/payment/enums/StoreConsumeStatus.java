package mannabom_server.manabom.domain.payment.enums;

/**
 * 스토어 측 소모 처리 상태.
 * Google Play 소모성 상품은 서버가 purchases.products.consume 을 호출해야 재구매가 가능하다.
 * Apple 은 클라이언트가 Transaction.finish() 를 호출하므로 서버 처리가 필요 없다(NOT_REQUIRED).
 */
public enum StoreConsumeStatus {
    NOT_REQUIRED,
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
