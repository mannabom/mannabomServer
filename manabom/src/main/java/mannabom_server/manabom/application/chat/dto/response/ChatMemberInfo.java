package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.enums.Gender;

@Getter
@Builder
public class ChatMemberInfo {
    private Long userId;
    private Long profileId;
    private String nickname;
    private String profileImageUrl;
    private Gender gender;

    public static ChatMemberInfo of(Profile profile, String displayImageUrl){
        return ChatMemberInfo.builder()
                .userId(profile.getUser().getUserId())
                .profileId(profile.getProfileId())
                .nickname(profile.getNickName())
                .profileImageUrl(displayImageUrl)
                .gender(profile.getGender())
                .build();
    }

}
