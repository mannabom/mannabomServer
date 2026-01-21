package mannabom_server.manabom.policy.model;

/**
 * 매번 DB를 조회해서 각 값들을 받아오면 리소스 낭비가 심하므로 메모리에 캐시 처리
 */
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuntimePolicySnapshot {

    private final Match match;
    private final Ting ting;
    private final Benefit benefit;

    @Getter
    @Builder
    public static class Match {
        private final int cooldownHours;
        private final int candidatePoolSize;
        private final int pickPoolSize;
    }

    @Getter
    @Builder
    public static class Ting {
        private final int vipThreshold;
        private final Cost cost;

        @Getter
        @Builder
        public static class Cost {
            private final int extraProfile;
            private final int extraProfileBundle5;
            private final int message;
            private final int like;
            private final int viewExtraPhoto;
            private final int viewScore;
            private final int viewLikedMeProfile;
            private final int viewHighScoreProfile;
        }
    }

    @Getter
    @Builder
    public static class Benefit {
        private final Membership membership;
        private final Vip vip;

        @Getter
        @Builder
        public static class Membership {
            private final int cycleExtraProfiles;
            private final int cycleFreeMessages;
            private final int cycleFreeLikes;
        }

        @Getter
        @Builder
        public static class Vip {
            private final int dailyExtraProfiles;
            private final int dailyFreeMessages;
            private final int dailyFreeLikes;
        }
    }
}
