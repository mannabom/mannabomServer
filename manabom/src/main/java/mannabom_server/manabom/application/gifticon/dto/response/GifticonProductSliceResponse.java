package mannabom_server.manabom.application.gifticon.dto.response;

import java.util.List;

public record GifticonProductSliceResponse(
        List<GifticonProductResponse> contents,
        Long nextCursor,
        boolean hasNext
) {
}
