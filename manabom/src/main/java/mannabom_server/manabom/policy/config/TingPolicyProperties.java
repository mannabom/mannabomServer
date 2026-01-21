package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ting.policy")
public class TingPolicyProperties {
    private int vipThreshold;
    private Cost cost = new Cost();

    @Getter @Setter
    public static class Cost{
        private int extraProfile;
        private int extraProfileBundle5;
        private int message;
        private int like;
        private int viewExtraPhoto;
        private int viewScore;
        private int viewLikedMeProfile;
        private int viewHighScoreProfile;
    }
}
