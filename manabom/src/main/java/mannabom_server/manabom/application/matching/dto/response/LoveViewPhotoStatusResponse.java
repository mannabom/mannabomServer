package mannabom_server.manabom.application.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.domain.matching.enums.LoveViewPhotoStatus;

@Getter
@AllArgsConstructor
public class LoveViewPhotoStatusResponse {
    private Long roomId;
    private LoveViewPhotoStatus status;
    private String systemMessage;
}
