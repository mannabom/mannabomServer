package mannabom_server.manabom.policy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "benefit.policy")
public class BenefitPolicyProperties {
    private Basic basic = new Basic();
    private Membership membership = new Membership();
    private Vip vip = new Vip();

    @Getter @Setter
    public static class Basic{
        private int dailyProfile;
        private int dailyLoveView;
    }

    @Getter @Setter
    public static class Membership{
        private int monthlyExtraProfiles;
        private int monthlyFreeMessages;
        private int monthlyFreeLikes;
    }
    @Getter @Setter
    public static class  Vip{
        private int dailyExtraProfiles;
        private int dailyFreeMessages;
        private int dailyFreeLikes;
    }
}
