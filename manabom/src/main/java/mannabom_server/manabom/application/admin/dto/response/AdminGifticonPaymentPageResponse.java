package mannabom_server.manabom.application.admin.dto.response;

import java.util.List;

public record AdminGifticonPaymentPageResponse(
        List<AdminGifticonPaymentResponse> contents,
        long totalCount,
        int totalPages,
        int page,
        int size
) {
}
