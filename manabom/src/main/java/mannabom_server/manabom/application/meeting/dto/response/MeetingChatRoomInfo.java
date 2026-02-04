package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.entity.User;

import java.util.List;

/*
* 동성 채팅방 정보
* */
@Getter
@Builder
public class MeetingChatRoomInfo {

    private Long roomId;
    @JsonUnwrapped
    private MeetingInfo meetingInfo;

    @Builder.Default
    private boolean isLeader = false;

    @Builder.Default
    private List<TeamMember> teamMembers = List.of();


    public static MeetingChatRoomInfo of(Long roomId,MeetingInfo meetingInfo, boolean isLeader, List<TeamMember> teamMembers){
        return MeetingChatRoomInfo.builder()
                .roomId(roomId)
                .meetingInfo(meetingInfo)
                .isLeader(isLeader)
                .teamMembers(teamMembers)
                .build();
    }



    @Getter
    @Builder
    public static class TeamMember{
        private Long userId;
        private String nickname;
        private String profileImage;
        private boolean isLeader;
    }


}
