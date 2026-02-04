package mannabom_server.manabom.application.meeting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.common.AgeRangeDto;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;
import mannabom_server.manabom.domain.meeting.enums.MeetingType;

/**
 * 미팅 방 생성 요청 DTO
 */

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class MeetingRoomCreateRequest {
    @NotNull(message = "방 이름은 필수입니다.")
    private String roomName;

    @Valid
    @NotNull(message = "지역 정보는 필수입니다.")
    private RegionDto region;

    @Valid
    @NotNull(message = "나이 범위 정보는 필수입니다.")
    private AgeRangeDto ageRange;

    @NotNull(message = "팀원 수 정보는 필수입니다.")
    @Min(value = 2, message = "최소 인원은 2명 입니다.")
    @Max(value = 4, message = "최대 인원은 4명 입니다.")
    private Integer maxMembers;

    @NotNull(message = "미팅 타입은 필수입니다.")
    private MeetingType meetingType;


    public MeetingRoomCreateRequest(String roomName, RegionDto region, AgeRangeDto ageRange, int maxMembers) {
        this.roomName = roomName;
        this.region = region;
        this.ageRange = ageRange;
        this.maxMembers = maxMembers;
    }
}
