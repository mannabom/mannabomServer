package mannabom_server.manabom.application.chat.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum SystemMessageType {
    MATCHING_STARTED(
            "매칭을 시작했어요",
            "팀장님이 매칭을 시작하셨어요."
    ),
    MATCHING_STOPPED(
            "매칭을 중단했어요",
            "팀장님이 매칭을 중단하셨어요."
    ),
    MATCH_FOUND(
            "미팅할 상대 팀을 찾았어요‼️✨",
            "수락 가능 시간 안에 팀원들과 상의해 주세요.\n"
                    + "팀장은 상단 헤더에서 수락을 확정해 주세요.\n"
                    + "수락 가능 시간이 지나면 자동 거절돼요."
    ),
    MATCH_REJECTED_BY_LEADER(
            "아쉽네요",
            "팀장님이 매칭을 거절했어요.\n새로운 상대 팀을 찾고 있어요."
    ),
    MATCH_REJECTED_BY_OPPONENT(
            "아쉽네요",
            "이번에는 상대 팀과 연결되지 않았어요.\n다시 매칭 신청을 눌러주세요!"
    ),
    MATCH_TIMED_OUT(
            "응답 시간이 지났어요",
            "수락 가능 시간이 지나 매칭이 자동으로 거절됐어요."
    ),
    OPPONENT_MATCH_TIMED_OUT(
            "아쉽네요",
            "상대 팀의 응답 시간이 지나 매칭이 취소됐어요.\n다시 매칭 신청을 눌러주세요!"
    ),
    MATCH_COMPLETED(
            "미팅이 성사되었어요 🎉🎉",
            "이 채팅방은 더 이상 이용하실 수 없습니다.\n"
                    + "새로운 채팅방으로 이동하여 상대 팀과 이야기를 나눠보세요!"
    ),
    CHAT_ROOM_CREATED(
            "매칭이 성사되었어요 🎉",
            "서로 인사를 나누고 즐거운 대화를 시작해 보세요."
    ),
    MEETING_VERIFICATION_STARTED(
            "만남인증 시작",
            "만남인증이 시작되었어요."
    ),
    MEETING_VERIFICATION_SUCCEEDED(
            "만남인증 성공",
            "만남인증이 완료되었어요."
    ),
    MEETING_VERIFICATION_FAILED(
            "만남인증 실패",
            "만남인증이 완료되지 않았어요."
    ),
    MEETING_CANCELLATION_VOTE_STARTED(
            "미팅 취소 투표",
            "미팅 취소 투표가 시작되었어요.\n"
                    + "모든 인원이 취소에 동의하면 채팅방이 사라지고 채팅방에서 사용된 팅은 위약금으로 제외하고 환불해드려요."
    ),
    MEETING_CANCELLATION_APPROVED(
            "미팅 취소 투표 결과",
            "모두가 동의하여 미팅과 채팅방이 취소되었어요."
    ),
    MEETING_CANCELLATION_REJECTED(
            "미팅 취소 투표 결과",
            "모두가 동의하지 않아 취소되지 않았어요."
    ),
    MEETING_CANCELLATION_EXPIRED(
            "미팅 취소 투표 결과",
            "투표 시간이 지나 미팅이 취소되지 않았어요."
    ),
    PHOTO_REQUESTED(
            "프로필 공개 요청",
            "수락할 시 서로의 프로필이 공개됩니다.\n헤더를 내려 수락하기 버튼을 눌러주세요."
    ),
    PHOTO_REQUEST_ACCEPTED(
            "서로의 프로필이 공개되었습니다! ✨",
            "서로의 프로필을 눌러 더 다양한 대화를 나눠보세요!"
    ),
    PHOTO_REQUEST_REJECTED(
            "프로필 공개가 거절되었어요",
            "아직 상대가 조심스러운가 봐요.\n더 대화를 나눠보세요!"
    );

    private final String defaultTitle;
    private final String defaultBody;

    public RenderedSystemMessage render(String actorNickname, Map<String, Object> data) {
        String nickname = StringUtils.hasText(actorNickname) ? actorNickname : "사용자";
        int participantCount = number(data, "participantCount");
        int verifiedParticipantCount = data != null && data.containsKey("verifiedParticipantCount")
                ? number(data, "verifiedParticipantCount")
                : participantCount;

        return switch (this) {
            case MATCHING_STARTED -> message(
                    defaultTitle,
                    "팀장 " + nickname + "님이 매칭을 시작하셨어요."
            );
            case MATCHING_STOPPED -> message(
                    defaultTitle,
                    "팀장 " + nickname + "님이 매칭을 중단하셨어요."
            );
            case MATCH_REJECTED_BY_LEADER -> message(
                    defaultTitle,
                    "팀장 " + nickname + "님이 매칭을 거절했어요.\n새로운 상대 팀을 찾고 있어요."
            );
            case MEETING_VERIFICATION_STARTED -> message(
                    defaultTitle,
                    nickname + "님이 만남인증을 시작했어요."
            );
            case MEETING_VERIFICATION_SUCCEEDED -> message(
                    defaultTitle,
                    nickname + "님의 참여로 만남인증이 완료되었어요.\n"
                            + "참여인원 : " + verifiedParticipantCount + "명"
            );
            case MEETING_VERIFICATION_FAILED -> message(
                    defaultTitle,
                    nickname + "님이 시작한 만남인증이 완료되지 않았어요.\n"
                            + "참여인원 : " + participantCount + "명"
            );
            case MEETING_CANCELLATION_VOTE_STARTED -> message(
                    defaultTitle,
                    nickname + "님이 미팅 취소 투표를 시작하셨어요!\n"
                            + "모든 인원이 취소에 동의하면 채팅방이 사라지고 채팅방에서 사용된 팅은 위약금으로 제외하고 환불해드려요."
            );
            case PHOTO_REQUESTED -> message(
                    nickname + "님이 프로필 공개를 요청했어요",
                    defaultBody
            );
            case PHOTO_REQUEST_ACCEPTED -> message(
                    defaultTitle,
                    nickname + "님이 프로필 공개 요청을 수락했어요.\n" + defaultBody
            );
            case PHOTO_REQUEST_REJECTED -> message(
                    defaultTitle,
                    nickname + "님이 아직은 조심스러운가 봐요.\n더 대화를 나눠보세요!"
            );
            default -> message(defaultTitle, defaultBody);
        };
    }

    private static int number(Map<String, Object> data, String key) {
        if (data == null) {
            return 0;
        }
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static RenderedSystemMessage message(String title, String body) {
        return new RenderedSystemMessage(title, body);
    }

    public record RenderedSystemMessage(String title, String body) {
    }
}
