package mannabom_server.manabom.application.meeting.dto.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageInfo {
    private boolean hasNext;
    private int totalPages;
    private Long totalElements;
    private int currentPage;
}
