package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@Builder
public class TeamMemberProfilesDto {
    private List<TeamMemberDetailDto> members;

    public static TeamMemberProfilesDto of(List<TeamMemberDetailDto> members){
        return TeamMemberProfilesDto.builder().members(members).build();
    }
    @Getter
    @Builder
    public static class TeamMemberDetailDto{
        private Long userId;
        private String nickname;
        private String profileImage;
        private Integer age;
        private String mbti;
        SmokingHabit smokingHabit;
        DrinkingHabit drinkingHabit;

    }
}

