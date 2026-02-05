package mannabom_server.manabom.application.like.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;

@Getter
@NoArgsConstructor
public class SendLikeRequestDto {
    @NotNull(message = "targetProfileId는 필수입니다.")
    private Long targetProfileId;

    @NotNull(message = "source는 필수입니다.")
    private LikeSource source;
}
