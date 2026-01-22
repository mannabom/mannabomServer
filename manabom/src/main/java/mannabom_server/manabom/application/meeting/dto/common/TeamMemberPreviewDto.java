package mannabom_server.manabom.application.meeting.dto.common;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.entity.User;

@Builder
@Getter
public class TeamMemberPreviewDto {
    private Long userId;
    private String profileImage;

    public static TeamMemberPreviewDto of(User user){
        return TeamMemberPreviewDto.builder()
                .userId(user.getUserId())
                .profileImage(user.getProfile().extractMainImageUrl())
                .build();
    }
}
