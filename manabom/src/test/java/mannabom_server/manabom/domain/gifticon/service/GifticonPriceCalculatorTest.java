package mannabom_server.manabom.domain.gifticon.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GifticonPriceCalculatorTest {

    @Test
    void calculateSalePriceAddsTenPercentAndRoundsUpToHundredWon() {
        GifticonPriceCalculator calculator = new GifticonPriceCalculator(
                BigDecimal.TEN,
                100
        );

        assertThat(calculator.calculateSalePrice(300)).isEqualTo(400);
        assertThat(calculator.calculateSalePrice(10_000)).isEqualTo(11_000);
        assertThat(calculator.calculateSalePrice(10_050)).isEqualTo(11_100);
    }

    @Test
    void calculateSalePriceRejectsNegativeProductPrice() {
        GifticonPriceCalculator calculator = new GifticonPriceCalculator(
                BigDecimal.TEN,
                100
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> calculator.calculateSalePrice(-1)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
