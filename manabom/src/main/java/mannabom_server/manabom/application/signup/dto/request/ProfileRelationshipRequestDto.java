package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.user.enums.BodyType;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;

import java.util.Map;

@Getter
@NoArgsConstructor
public class ProfileRelationshipRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;  // 🔥 kakaoId 대신 profileId 사용

    @NotNull(message = "키는 필수입니다.")
    private Integer height;

    @NotNull(message = "체형은 필수입니다.")
    private BodyType bodyType;

    @Valid
    @NotNull(message = "지역 정보는 필수입니다.")
    private RegionDto region;

    @NotBlank(message = "MBTI는 필수입니다.")
    @Pattern(regexp = "^[EI][SN][TF][JP]$", message = "올바른 MBTI 형식이 아닙니다.")
    private String mbti;

    private SmokingHabit smokingHabit;
    private DrinkingHabit drinkingHabit;

    @NotBlank(message = "자기소개는 필수입니다.")
    @Size(min = 100, message = "자기소개는 100자 이상 작성해주세요.")
    private String selfIntroduction;

    @NotBlank(message = "나를 설레게 하는 이성의 매력은 필수입니다.")
    @Size(min = 30, message = "나를 설레게 하는 이성의 매력은 30자 이상 작성해주세요.")
    private String attractivePartnerTrait;

    @NotBlank(message = "연인에게 꼭 바라는 한 가지는 필수입니다.")
    @Size(min = 30, message = "연인에게 꼭 바라는 한 가지는 30자 이상 작성해주세요.")
    private String desiredPartnerTrait;

    // 선택 질문들
    private Map<String, String> optionalAnswers;

    // 연애관 이지선다 질문들
    @NotNull(message = "연애관 선택은 필수입니다.")
    private Map<String, String> relationshipChoices;

    @Getter
    @NoArgsConstructor
    public static class RegionDto {
        @NotBlank(message = "시/도는 필수입니다.")
        private String sido;

        @NotBlank(message = "구는 필수입니다.")
        private String sigungu;
    }
}
