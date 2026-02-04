package mannabom_server.manabom.application.userInfo.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.university.entity.University;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.BodyType;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.time.LocalDate;

@Getter
@Builder
public class ProfileDto {
    private Long profileId;
    private Gender gender;
    private Integer height;
    private BodyType bodyType;
    @JsonUnwrapped
    private RegionDto region;
    private String nickName;
    private Double grade;
    private LocalDate birthDate;
    private String mbti;
    private DrinkingHabit alcohol;
    private SmokingHabit smoking;
    private String university;
    private String email;

    public static ProfileDto of(Profile profile){
        return ProfileDto.builder()
                .profileId(profile.getProfileId())
                .gender(profile.getGender())
                .height(profile.getHeight())
                .bodyType(profile.getBodyType())
                .region(
                        RegionDto.from(profile.getRegion())
                )
                .university(profile.getUniversity().getName())
                .nickName(profile.getNickName())
                .alcohol(profile.getAlcohol())
                .smoking(profile.getSmoking())
                .email(profile.getEmail())
                .grade(profile.getGrade())
                .birthDate(profile.getBirthDate())
                .mbti(profile.getMbti())
                .build();
    }
}
