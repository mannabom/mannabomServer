package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.enums.Gender;

import java.util.List;

/*
 * 이성과 매칭된 채팅방 정보
 * */
@Getter
@Builder
public class MatchedChatRoomInfo {
    private Long roomId;
    private List<Participant> participants;
    private int totalMembers;

    public static MatchedChatRoomInfo of(Long roomId, List<Participant> participants){
        return MatchedChatRoomInfo.builder()
                .roomId(roomId)
                .participants(participants)
                .totalMembers(participants.size())
                .build();
    }


    public record Participant(
        Long userId,
        Long profileId,
        Gender gender,
        String nickname,
        String profileImg
    ){}
}
