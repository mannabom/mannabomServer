package mannabom_server.manabom.domain.gifticon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GifticonPriceCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal markupPercent;
    private final int roundUnit;

    public GifticonPriceCalculator(BigDecimal markupPercent, int roundUnit) {
        if (markupPercent == null || markupPercent.signum() < 0) {
            throw new IllegalArgumentException("markup-percent는 0 이상이어야 합니다.");
        }
        if (roundUnit <= 0) {
            throw new IllegalArgumentException("round-unit은 1 이상이어야 합니다.");
        }
        this.markupPercent = markupPercent;
        this.roundUnit = roundUnit;
    }

    public int calculateSalePrice(int productPrice) {
        return calculateSalePrice(productPrice, markupPercent);
    }

    public int calculateSalePrice(int productPrice, BigDecimal appliedMarkupPercent) {
        if (productPrice < 0) {
            throw new IllegalArgumentException("기프티콘 원화 가격은 0 이상이어야 합니다.");
        }
        if (appliedMarkupPercent == null || appliedMarkupPercent.signum() < 0) {
            throw new IllegalArgumentException("markup-percent는 0 이상이어야 합니다.");
        }

        BigDecimal markupMultiplier = BigDecimal.ONE.add(
                appliedMarkupPercent.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
        );
        BigDecimal rawSalePrice = BigDecimal.valueOf(productPrice)
                .multiply(markupMultiplier)
                .setScale(0, RoundingMode.CEILING);
        return rawSalePrice
                .divide(BigDecimal.valueOf(roundUnit), 0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(roundUnit))
                .intValueExact();
    }
}
