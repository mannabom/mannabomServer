package mannabom_server.manabom.application.pushService.dto.response;

import java.util.List;

public record PushBatchResult(int successCount, int failureCount, List<String> invalidTokens) {}
