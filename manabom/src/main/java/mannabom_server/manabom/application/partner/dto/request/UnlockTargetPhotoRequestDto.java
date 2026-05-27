package mannabom_server.manabom.application.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UnlockTargetPhotoRequestDto {
    @NotNull(message = "targetProfileId는 필수입니다.")
    private Long targetProfileId;

    @NotNull(message = "photoId는 필수입니다.")
    private Long photoId;
}
