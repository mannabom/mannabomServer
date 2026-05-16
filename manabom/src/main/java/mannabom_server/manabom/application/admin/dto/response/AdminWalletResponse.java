package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminWalletResponse {
    private final Long userId;
    private final int ting;
    private final int eventTing;
}
