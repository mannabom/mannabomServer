package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminWalletResponse {
    private final Long userId;
    private final int ting;
    private final int eventTing;
    private final boolean membershipActive;
    private final LocalDateTime membershipCycleStartAt;
    private final LocalDateTime membershipActiveUntil;
}
