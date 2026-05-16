package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminUserListResponse {
    private final List<AdminUserSummaryResponse> users;
    private final long totalCount;
    private final int totalPages;
    private final int page;
    private final int size;
}
