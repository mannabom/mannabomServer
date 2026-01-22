package mannabom_server.manabom.application.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MeetingRoomJoinByCodeRequest {

    @NotNull(message = "미팅방 초대코드는 필수입니다.")
    private String roomCode;
}
