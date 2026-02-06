package mannabom_server.manabom.application.matching.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecommendedTodayProfileListResponseDto {
    private List<ProfileMatchConditionResponseDto> recommendedTodayProfileList;
}
