package mannabom_server.manabom.application.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.AgeRangeDto;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;

import java.util.List;

@AllArgsConstructor
@Getter
public class MeetingRoomsSearchRequest {
    @NotNull(message = "지역 정보는 필수입니다.")
    private RegionDto region;
    @NotNull(message = "나이 범위 정보는 필수입니다.")
    private AgeRangeDto ageRange;
    @NotNull(message = "멤버 수 정보는 필수입니다.")
    private List<Integer> memberCounts;
}
