package mannabom_server.manabom.application.meeting.dto.common;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.entity.User;

import java.util.List;

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

        public static TeamMember of(User user, boolean isLeader){
            return TeamMember.builder()
                    .userId(user.getUserId())
                    .nickname(user.getProfile().getNickName())
                    .isLeader(isLeader)
                    .profileImage(user.getProfile().extractMainImageUrl())
                    .build();
        }
    }


}
