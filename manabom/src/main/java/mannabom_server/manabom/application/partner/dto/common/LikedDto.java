package mannabom_server.manabom.application.partner.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;

@Getter
@AllArgsConstructor
public class LikedDto {
    boolean sent;
    LikeStatus likeStatus;
}
