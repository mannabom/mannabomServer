package mannabom_server.manabom.application.meeting.dto.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfo{
    @Builder.Default
    private Integer currentCount =1;
    private Integer maxCount;
}