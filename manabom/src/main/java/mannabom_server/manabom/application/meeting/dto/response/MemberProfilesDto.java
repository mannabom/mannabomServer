package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.List;

@Getter
@Builder
public class MemberProfilesDto{
    private List<MemberDetailDto> members;

    public static MemberProfilesDto of(List<MeetingMember> mms){
        return MemberProfilesDto.builder()
                .members(
                        mms.stream().map(
                mm -> MemberDetailDto.of(mm.getUser())
                        ).toList()
                ).build();
    }

    @Getter
    @Builder
    public static class MemberDetailDto{
        private Long userId;
        private String profileImage;
        private Integer age;
        private String mbti;
        SmokingHabit smokingHabit;
        DrinkingHabit drinkingHabit;

        public static MemberDetailDto of(User user){
            Profile profile = user.getProfile();
            return MemberDetailDto.builder()
                    .userId(user.getUserId())
                    .profileImage(profile.extractMainImageUrl())
                    .age(profile.computeAge())
                    .mbti(profile.getMbti())
                    .drinkingHabit(profile.getAlcohol())
                    .smokingHabit(profile.getSmoking())
                    .build();
        }
    }
}

