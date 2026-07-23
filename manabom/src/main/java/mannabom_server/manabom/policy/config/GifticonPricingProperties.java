package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "gifticon.pricing")
public class GifticonPricingProperties {

    private BigDecimal wonPerTing = BigDecimal.valueOf(100);
    private BigDecimal markupPercent = BigDecimal.ZERO;
    private int roundUnit = 1;
}
