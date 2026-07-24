package mannabom_server.manabom.application.admin.dto.response;

import java.util.List;

public record AdminGifticonProductSliceResponse(
        List<AdminGifticonProductResponse> contents,
        Long nextCursor,
        boolean hasNext
) {
}
