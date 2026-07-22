package mannabom_server.manabom.application.chat.message;

public final class SystemMessageContent {

    public static final String MATCHING_STARTED =
            "팀장님이 매칭을 시작했어요. 상대 팀을 찾고 있어요.";
    public static final String MATCHING_STOPPED =
            "팀장님이 매칭을 중단했어요.";
    public static final String MATCH_FOUND =
            "미팅할 상대 팀을 찾았어요. 팀장님의 결정을 기다리고 있어요.";
    public static final String MATCH_REJECTED_BY_LEADER =
            "팀장님이 매칭을 거절했어요. 새로운 상대 팀을 찾고 있어요.";
    public static final String MATCH_REJECTED_BY_OPPONENT =
            "상대 팀이 매칭을 거절했어요. 새로운 상대 팀을 찾고 있어요.";
    public static final String MATCH_TIMED_OUT =
            "응답 시간이 지나 매칭이 자동으로 거절됐어요.";
    public static final String OPPONENT_MATCH_TIMED_OUT =
            "상대 팀의 응답 시간이 지나 매칭이 취소됐어요.";
    public static final String MATCH_COMPLETED =
            "상대 팀이 수락해 미팅이 성사됐어요. 새로운 채팅방에서 대화를 시작해 보세요.";
    public static final String CHAT_ROOM_CREATED =
            "🎉 매칭이 성사되었습니다! 서로 인사를 나눠보세요.";
    public static final String MEETING_VERIFICATION_STARTED =
            "누군가 만남 인증을 시작했어요.";
    public static final String MEETING_VERIFICATION_SUCCEEDED =
            "만남 인증에 성공했어요.";
    public static final String MEETING_VERIFICATION_FAILED =
            "만남 인증에 실패했어요.";
    public static final String MEETING_CANCELLATION_VOTE_STARTED =
            "미팅 취소 투표가 시작됐어요.";
    public static final String MEETING_CANCELLATION_APPROVED =
            "미팅 취소 투표가 가결되어 미팅이 취소됐어요.";
    public static final String MEETING_CANCELLATION_REJECTED =
            "미팅 취소 투표가 부결됐어요.";
    public static final String MEETING_CANCELLATION_EXPIRED =
            "미팅 취소 투표가 응답 시간 초과로 종료됐어요.";
    public static final String PHOTO_REQUESTED =
            "프로필 사진 공개 요청이 전송되었어요.";
    public static final String PHOTO_REQUEST_ACCEPTED =
            "프로필 사진 공개 요청이 수락되었어요.";
    public static final String PHOTO_REQUEST_REJECTED =
            "프로필 사진 공개 요청이 거절되었어요.";

    private SystemMessageContent() {
    }
}
