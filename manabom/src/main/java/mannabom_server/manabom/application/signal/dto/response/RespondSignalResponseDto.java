package mannabom_server.manabom.application.signal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RespondSignalResponseDto {
    private boolean accepted;
    private Long chatRoomId;
    private String status;
    private String gifticonOrderStatus;
}
