package mannabom_server.manabom.application.chat.message;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMessageTypeTest {

    @Test
    void rendersLeaderNicknameInMatchingMessage() {
        SystemMessageType.RenderedSystemMessage message =
                SystemMessageType.MATCHING_STARTED.render("봄이", Map.of());

        assertThat(message.title()).isEqualTo("매칭을 시작했어요");
        assertThat(message.body()).isEqualTo("팀장 봄이님이 매칭을 시작하셨어요.");
    }

    @Test
    void rendersVerificationCompleterAndParticipantCount() {
        SystemMessageType.RenderedSystemMessage message =
                SystemMessageType.MEETING_VERIFICATION_SUCCEEDED.render(
                        "민수",
                        Map.of(
                                "submittedCount", 4,
                                "verifiedParticipantCount", 3
                        )
                );

        assertThat(message.title()).isEqualTo("만남인증 성공");
        assertThat(message.body())
                .isEqualTo("민수님의 참여로 만남인증이 완료되었어요.\n참여인원 : 3명");
    }

    @Test
    void rendersProfileRequesterNickname() {
        SystemMessageType.RenderedSystemMessage message =
                SystemMessageType.PHOTO_REQUESTED.render("지수", Map.of());

        assertThat(message.title()).isEqualTo("지수님이 프로필 공개를 요청했어요");
        assertThat(message.body()).contains("수락할 시 서로의 프로필이 공개됩니다");
    }
}
