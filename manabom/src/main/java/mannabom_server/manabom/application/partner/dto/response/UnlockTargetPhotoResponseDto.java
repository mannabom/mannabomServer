package mannabom_server.manabom.application.partner.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnlockTargetPhotoResponseDto {
    private int tingRemains;
    private int eventTingRemains;
}
