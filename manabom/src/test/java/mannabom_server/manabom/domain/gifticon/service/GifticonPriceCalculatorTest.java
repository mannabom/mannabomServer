package mannabom_server.manabom.domain.gifticon.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GifticonPriceCalculatorTest {

    @Test
    void calculateTingPriceAppliesConversionMarkupAndCeilingUnit() {
        GifticonPriceCalculator calculator = new GifticonPriceCalculator(
                BigDecimal.TEN,
                BigDecimal.valueOf(5),
                10
        );

        int tingPrice = calculator.calculateTingPrice(300);

        assertThat(tingPrice).isEqualTo(40);
    }
}
