package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptMatchDataDto {
    MatchedChatRoomInfo chatRoom;
    MatchingStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    Instant decisionDeadline;

    public static AcceptMatchDataDto ofMatched(MatchedChatRoomInfo chatRoom){
        return AcceptMatchDataDto.builder()
                .chatRoom(chatRoom)
                .status(MatchingStatus.SUCCEEDED)
                .build();
    }

    public static AcceptMatchDataDto ofWaiting(Instant deadline){
        return AcceptMatchDataDto.builder()
                .decisionDeadline(deadline)
                .status(MatchingStatus.PENDING)
                .build();
    }
}
