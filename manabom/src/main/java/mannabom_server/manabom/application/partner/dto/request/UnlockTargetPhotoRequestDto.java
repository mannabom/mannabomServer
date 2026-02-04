package mannabom_server.manabom.application.partner.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UnlockTargetPhotoRequestDto {
    private Long targetProfileId;
    private Long photoId;
}
