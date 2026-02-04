package mannabom_server.manabom.domain.meeting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventName {
    CONNECTED("CONNECTED"),
    MATCHING_STATUS("MATCHING_STATUS"),
    MATCH_FOUND("MATCH_FOUND"),
    MATCHING_FAILED("MATCHING_FAILED"),
    DECISION_RESULT("DECISION_RESULT"),
    MATCHING_COMPLETED("MATCHING_COMPLETED");

    private final String description;
}
