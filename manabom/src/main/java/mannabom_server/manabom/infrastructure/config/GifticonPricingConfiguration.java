package mannabom_server.manabom.infrastructure.config;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.gifticon.service.GifticonPriceCalculator;
import mannabom_server.manabom.policy.config.GifticonPricingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GifticonPricingConfiguration {

    private final GifticonPricingProperties properties;

    @Bean
    public GifticonPriceCalculator gifticonPriceCalculator() {
        return new GifticonPriceCalculator(
                properties.getWonPerTing(),
                properties.getMarkupPercent(),
                properties.getRoundUnit()
        );
    }
}
