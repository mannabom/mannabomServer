package mannabom_server.manabom.application.meeting.service;

public interface MeetingMatchingTimeoutScheduler {
    void scheduleExpiration(Long matchId);
}
