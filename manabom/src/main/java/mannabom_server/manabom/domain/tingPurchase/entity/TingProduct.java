package mannabom_server.manabom.domain.tingPurchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;

/**
 * 인앱결제로 판매하는 팅 패키지.
 * 스토어(App Store Connect / Play Console)에 등록한 상품 ID와 서버 지급량을 연결한다.
 */
@Getter
@Entity
@Table(name = "ting_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TingProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ting_product_id")
    private Long tingProductId;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "ting_amount", nullable = false)
    private int tingAmount;

    @Column(name = "bonus_event_ting", nullable = false)
    private int bonusEventTing;

    @Column(name = "display_price_krw", nullable = false)
    private int displayPriceKrw;

    @Column(name = "apple_product_id", unique = true, length = 100)
    private String appleProductId;

    @Column(name = "google_product_id", unique = true, length = 100)
    private String googleProductId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public TingProduct(
            String productCode,
            String name,
            int tingAmount,
            int bonusEventTing,
            int displayPriceKrw,
            String appleProductId,
            String googleProductId,
            boolean active,
            int sortOrder
    ) {
        this.productCode = requireCode(productCode);
        update(name, tingAmount, bonusEventTing, displayPriceKrw, appleProductId, googleProductId, active, sortOrder);
    }

    public void update(
            String name,
            int tingAmount,
            int bonusEventTing,
            int displayPriceKrw,
            String appleProductId,
            String googleProductId,
            boolean active,
            int sortOrder
    ) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("상품 이름은 1~100자여야 합니다.");
        }
        if (tingAmount <= 0) {
            throw new IllegalArgumentException("지급 팅 수량은 0보다 커야 합니다.");
        }
        if (bonusEventTing < 0) {
            throw new IllegalArgumentException("보너스 이벤트 팅은 음수일 수 없습니다.");
        }
        if (displayPriceKrw < 0) {
            throw new IllegalArgumentException("표시 가격은 음수일 수 없습니다.");
        }
        this.name = name.trim();
        this.tingAmount = tingAmount;
        this.bonusEventTing = bonusEventTing;
        this.displayPriceKrw = displayPriceKrw;
        this.appleProductId = normalizeStoreProductId(appleProductId);
        this.googleProductId = normalizeStoreProductId(googleProductId);
        this.active = active;
        this.sortOrder = sortOrder;
    }

    /**
     * 스토어별 상품 ID를 반환한다. 해당 스토어에 등록되지 않은 상품이면 예외.
     */
    public String storeProductId(PurchaseStore store) {
        if (store == null) {
            throw new IllegalArgumentException("스토어 정보는 필수입니다.");
        }
        String id = store == PurchaseStore.APPLE ? appleProductId : googleProductId;
        if (id == null) {
            throw new IllegalStateException(store.getDescription() + "에 등록되지 않은 팅 상품입니다.");
        }
        return id;
    }

    public boolean isPurchasableAt(PurchaseStore store) {
        return active
                && (store == PurchaseStore.APPLE ? appleProductId != null : googleProductId != null);
    }

    private static String requireCode(String code) {
        if (code == null || !code.matches("[A-Z0-9_]{2,50}")) {
            throw new IllegalArgumentException("상품 코드는 대문자/숫자/밑줄 2~50자여야 합니다.");
        }
        return code;
    }

    private static String normalizeStoreProductId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("스토어 상품 ID는 100자를 초과할 수 없습니다.");
        }
        return trimmed;
    }
}
