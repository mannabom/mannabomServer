package mannabom_server.manabom.application.admin.dto.response;

import java.time.Instant;

public record AdminGifticonSyncResponse(
        int synchronizedCount,
        Instant synchronizedAt
) {
}
