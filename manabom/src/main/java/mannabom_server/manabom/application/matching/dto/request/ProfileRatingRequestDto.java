package mannabom_server.manabom.application.matching.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileRatingRequestDto {
    @NotNull(message = "targetProfileId는 필수입니다.")
    Long targetProfileId;

    @NotNull(message = "score는 필수입니다.")
    @Min(value = 1, message = "score는 1 이상이어야 합니다.")
    @Max(value = 5, message = "score는 5 이하여야 합니다.")
    Integer score;
}
