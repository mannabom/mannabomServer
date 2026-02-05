package mannabom_server.manabom.application.like.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendLikeRequestDto {
    @NotNull(message = "targetProfileId는 필수입니다.")
    private Long targetProfileId;
}
