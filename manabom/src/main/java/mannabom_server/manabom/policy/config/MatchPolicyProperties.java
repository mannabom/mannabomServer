package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "match.policy")
public class MatchPolicyProperties {
    private int cooldownHours;
    private int candidatePoolSize;
    private int pickPoolSize;
}
