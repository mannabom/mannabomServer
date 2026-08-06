package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "gifticon.pricing")
public class GifticonPricingProperties {

    private BigDecimal markupPercent = BigDecimal.TEN;
    private int roundUnit = 100;
}
