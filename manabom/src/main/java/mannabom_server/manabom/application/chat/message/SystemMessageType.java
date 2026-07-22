package mannabom_server.manabom.application.chat.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemMessageType {
    MATCHING_STARTED(
            SystemMessageContent.MATCHING_STARTED,
            "매칭 시작",
            "팀장님이 매칭을 시작했어요."
    ),
    MATCHING_STOPPED(
            SystemMessageContent.MATCHING_STOPPED,
            "매칭 중단",
            "팀장님이 매칭을 중단했어요."
    ),
    MATCH_FOUND(
            SystemMessageContent.MATCH_FOUND,
            "미팅 상대 팀 발견",
            "미팅할 상대 팀을 찾았어요. 팀장님의 결정을 기다리고 있어요."
    ),
    MATCH_REJECTED_BY_LEADER(
            SystemMessageContent.MATCH_REJECTED_BY_LEADER,
            "매칭 거절",
            "팀장님이 매칭을 거절했어요."
    ),
    MATCH_REJECTED_BY_OPPONENT(
            SystemMessageContent.MATCH_REJECTED_BY_OPPONENT,
            "매칭 거절",
            "상대 팀이 매칭을 거절했어요."
    ),
    MATCH_TIMED_OUT(
            SystemMessageContent.MATCH_TIMED_OUT,
            "매칭 응답 시간 종료",
            "응답 시간이 지나 매칭이 자동으로 거절됐어요."
    ),
    OPPONENT_MATCH_TIMED_OUT(
            SystemMessageContent.OPPONENT_MATCH_TIMED_OUT,
            "매칭 응답 시간 종료",
            "상대 팀의 응답 시간이 지나 매칭이 취소됐어요."
    ),
    MATCH_COMPLETED(
            SystemMessageContent.MATCH_COMPLETED,
            "미팅 매칭 성사",
            "상대 팀이 수락해 미팅 매칭이 성사됐어요."
    ),
    CHAT_ROOM_CREATED(
            SystemMessageContent.CHAT_ROOM_CREATED,
            "새 채팅방 생성",
            "매칭이 성사됐어요. 지금 대화를 시작해 보세요."
    ),
    MEETING_VERIFICATION_STARTED(
            SystemMessageContent.MEETING_VERIFICATION_STARTED,
            "만남 인증 시작",
            "채팅방에서 만남 인증이 시작됐어요."
    ),
    MEETING_VERIFICATION_SUCCEEDED(
            SystemMessageContent.MEETING_VERIFICATION_SUCCEEDED,
            "만남 인증 성공",
            "만남 인증이 성공했어요."
    ),
    MEETING_VERIFICATION_FAILED(
            SystemMessageContent.MEETING_VERIFICATION_FAILED,
            "만남 인증 실패",
            "제한 시간 안에 조건을 충족하지 못해 만남 인증이 종료됐어요."
    ),
    MEETING_CANCELLATION_VOTE_STARTED(
            SystemMessageContent.MEETING_CANCELLATION_VOTE_STARTED,
            "미팅 취소 투표 시작",
            "미팅 취소 투표가 시작됐어요."
    ),
    MEETING_CANCELLATION_APPROVED(
            SystemMessageContent.MEETING_CANCELLATION_APPROVED,
            "미팅 취소 결정",
            "미팅 취소 투표가 가결됐어요."
    ),
    MEETING_CANCELLATION_REJECTED(
            SystemMessageContent.MEETING_CANCELLATION_REJECTED,
            "미팅 취소 결정",
            "미팅 취소 투표가 부결됐어요."
    ),
    MEETING_CANCELLATION_EXPIRED(
            SystemMessageContent.MEETING_CANCELLATION_EXPIRED,
            "미팅 취소 투표 종료",
            "미팅 취소 투표가 응답 시간 초과로 종료됐어요."
    ),
    PHOTO_REQUESTED(
            SystemMessageContent.PHOTO_REQUESTED,
            "프로필 공개 요청",
            "상대방이 프로필 공개를 요청했어요."
    ),
    PHOTO_REQUEST_ACCEPTED(
            SystemMessageContent.PHOTO_REQUEST_ACCEPTED,
            "프로필 공개 성사",
            "프로필 공개 요청이 수락됐어요."
    ),
    PHOTO_REQUEST_REJECTED(
            SystemMessageContent.PHOTO_REQUEST_REJECTED,
            "프로필 공개 거절",
            "프로필 공개 요청이 거절됐어요."
    );

    private final String content;
    private final String notificationTitle;
    private final String notificationBody;
}
