package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPushResponse {
    private final int targetTokenCount;
    private final int successCount;
    private final int failureCount;
    private final int invalidTokenCount;
}
