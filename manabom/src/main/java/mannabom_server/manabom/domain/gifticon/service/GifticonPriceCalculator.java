package mannabom_server.manabom.domain.gifticon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GifticonPriceCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal wonPerTing;
    private final BigDecimal markupPercent;
    private final int roundUnit;

    public GifticonPriceCalculator(
            BigDecimal wonPerTing,
            BigDecimal markupPercent,
            int roundUnit
    ) {
        if (wonPerTing == null || wonPerTing.signum() <= 0) {
            throw new IllegalArgumentException("won-per-ting은 0보다 커야 합니다.");
        }
        if (markupPercent == null || markupPercent.signum() < 0) {
            throw new IllegalArgumentException("markup-percent는 0 이상이어야 합니다.");
        }
        if (roundUnit <= 0) {
            throw new IllegalArgumentException("round-unit은 1 이상이어야 합니다.");
        }
        this.wonPerTing = wonPerTing;
        this.markupPercent = markupPercent;
        this.roundUnit = roundUnit;
    }

    public int calculateTingPrice(int productPrice) {
        if (productPrice < 0) {
            throw new IllegalArgumentException("기프티콘 원화 가격은 0 이상이어야 합니다.");
        }

        BigDecimal markupMultiplier = BigDecimal.ONE.add(
                markupPercent.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
        );
        BigDecimal rawTingPrice = BigDecimal.valueOf(productPrice)
                .multiply(markupMultiplier)
                .divide(wonPerTing, 10, RoundingMode.CEILING);

        BigDecimal roundedUnits = rawTingPrice
                .divide(BigDecimal.valueOf(roundUnit), 0, RoundingMode.CEILING);
        return roundedUnits
                .multiply(BigDecimal.valueOf(roundUnit))
                .intValueExact();
    }
}
